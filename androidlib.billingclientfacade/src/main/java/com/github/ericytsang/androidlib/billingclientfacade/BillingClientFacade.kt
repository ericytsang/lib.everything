package com.github.ericytsang.androidlib.billingclientfacade

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener
import com.github.ericytsang.androidlib.core.forceExhaustiveWhen
import com.github.ericytsang.androidlib.core.postOnUiThread
import com.github.ericytsang.lib.optional.Opt
import com.github.ericytsang.lib.prop.DataProp
import com.github.ericytsang.lib.prop.RaiiProp
import com.github.ericytsang.lib.prop.ReadOnlyProp
import com.github.ericytsang.lib.prop.value
import java.io.Closeable
import java.lang.IllegalArgumentException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// todo refine, and abstract to separate library!
class BillingClientFacade
private constructor(
        context:Context,
        val skus:List<String>)
    :Closeable
{
    companion object
    {
        private val instanceLock = ReentrantLock()
        private var instance:BillingClientFacade? = null

        fun instance(context:Context,skus:List<String>) = instanceLock.withLock()
        {
            val instance = instance?:BillingClientFacade(context.applicationContext,skus)
            this.instance = instance
            require(skus == instance.skus)
            instance
        }
    }

    // updated every time when connected to google, and ready to sell stuff
    // after a transaction
    val onPurchaseStateChanged:ReadOnlyProp<Unit,Int> get() = _onPurchaseStateChanged
    private val _onPurchaseStateChanged = DataProp(0)

    // IapListings

    interface IapListings
    {
        fun details(sku:String):SkuDetails?
        fun isPurchased(sku:String):Boolean
    }

    // Opened

    private val doWhenReadyToSellStuff = LinkedBlockingQueue<(IapListings)->Unit>()

    private val opened = RaiiProp(Opt.some(Opened(this,context)))

    override fun close()
    {
        opened.value = {Opt.none()}
    }

    // client functions

    fun buy(activity:Activity,sku:String)
    {
        opened.value.invoke().opt
                ?.connected?.value?.invoke()?.opt
                ?.offers?.value?.invoke()?.opt
                ?.readyToSellStuff?.value?.invoke()?.opt
                ?.buy(activity,sku)
    }

    fun consume(sku:String)
    {
        opened.value.invoke().opt
                ?.connected?.value?.invoke()?.opt
                ?.offers?.value?.invoke()?.opt
                ?.readyToConsumeStuff?.value?.invoke()?.opt
                ?.consume(sku)
    }

    fun refreshPurchases()
    {
        opened.value.invoke().opt?.refreshPurchases()
    }

    fun doWhenListingsLoaded(block:(IapListings)->Unit)
    {
        doWhenReadyToSellStuff += block
        val listings = opened.value.invoke().opt
                ?.connected?.value?.invoke()?.opt
                ?.offers?.value?.invoke()?.opt
                ?.readyToSellStuff?.value?.invoke()?.opt
        if (listings != null)
        {
            generateSequence {doWhenReadyToSellStuff.poll()}
                    .forEach {it(listings)}
        }
    }

    // try to establish a connection to the billing service
    private class Opened(
            val billingClientFacade:BillingClientFacade,
            _androidContext:Context)
        :Closeable,BillingClientStateListener,PurchasesUpdatedListener,ConsumeResponseListener
    {
        val connected = RaiiProp(Opt.none<Connected>())

        val androidContext = _androidContext.applicationContext

        private val billingClient = BillingClient.newBuilder(androidContext)
                .setListener(this)
                .build()

        init
        {
            postOnUiThread()
            {
                billingClient.startConnection(this)
            }
        }

        override fun close()
        {
            connected.value = {Opt.none()}
            billingClient.endConnection()
        }

        override fun onConsumeResponse(responseCode:Int,purchaseToken:String?)
        {
            refreshPurchases()
        }

        override fun onPurchasesUpdated(responseCode:Int,purchases:MutableList<Purchase>?)
        {
            refreshPurchases()
        }

        fun refreshPurchases()
        {
            if (billingClientFacade.opened.value.invoke().opt != this) return
            connected.value = {Opt.some(Connected(this,billingClient))}
        }

        override fun onBillingServiceDisconnected()
        {
            if (billingClientFacade.opened.value.invoke().opt != this) return
            onBillingSetupFinished(BillingClient.BillingResponse.SERVICE_DISCONNECTED)
        }

        override fun onBillingSetupFinished(responseCode:Int)
        {
            if (billingClientFacade.opened.value.invoke().opt != this) return
            when(responseCode)
            {
                // connection failed
                BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED,
                BillingClient.BillingResponse.SERVICE_DISCONNECTED,
                BillingClient.BillingResponse.USER_CANCELED,
                BillingClient.BillingResponse.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponse.BILLING_UNAVAILABLE,
                BillingClient.BillingResponse.ITEM_UNAVAILABLE,
                BillingClient.BillingResponse.DEVELOPER_ERROR,
                BillingClient.BillingResponse.ERROR,
                BillingClient.BillingResponse.ITEM_ALREADY_OWNED,
                BillingClient.BillingResponse.ITEM_NOT_OWNED ->
                {
                    billingClientFacade.opened.value = {Opt.of(Opened(billingClientFacade,androidContext))}
                }

                // billing client connected
                BillingClient.BillingResponse.OK ->
                {
                    connected.value = {Opt.some(Connected(this,billingClient))}
                }
                else -> throw IllegalArgumentException("unknown billingResponseCode: $responseCode")
            }.forceExhaustiveWhen
        }
    }

    // query for the items that are one being sold
    private class Connected(
            val opened:Opened,
            val billingClient:BillingClient)
        :Closeable,SkuDetailsResponseListener
    {
        val offers = RaiiProp(Opt.none<Offers>())

        init
        {
            billingClient.querySkuDetailsAsync(
                    SkuDetailsParams.newBuilder()
                            .setType(BillingClient.SkuType.INAPP)
                            .setSkusList(opened.billingClientFacade.skus)
                            .build(),
                    this)
        }

        override fun close()
        {
            offers.value = {Opt.none()}
        }

        override fun onSkuDetailsResponse(responseCode:Int,skuDetailsList:MutableList<SkuDetails>?)
        {
            if (opened.connected.value.invoke().opt != this) return
            when(responseCode)
            {
                // connection failed
                BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED,
                BillingClient.BillingResponse.SERVICE_DISCONNECTED,
                BillingClient.BillingResponse.USER_CANCELED,
                BillingClient.BillingResponse.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponse.BILLING_UNAVAILABLE,
                BillingClient.BillingResponse.ITEM_UNAVAILABLE,
                BillingClient.BillingResponse.DEVELOPER_ERROR,
                BillingClient.BillingResponse.ERROR,
                BillingClient.BillingResponse.ITEM_ALREADY_OWNED,
                BillingClient.BillingResponse.ITEM_NOT_OWNED ->
                {
                    with(opened)
                    {
                        billingClientFacade.opened.value = {Opt.of(Opened(billingClientFacade,androidContext))}
                    }
                }

                // offers received
                BillingClient.BillingResponse.OK ->
                {
                    offers.value = {
                        Opt.some(Offers(this,skuDetailsList
                                ?: listOf()))
                    }
                }
                else -> throw IllegalArgumentException("unknown billingResponseCode: $responseCode")
            }.forceExhaustiveWhen
        }
    }

    // query for purchase history
    private class Offers(
            val connected:Connected,
            val offers:List<SkuDetails>)
        :Closeable,PurchaseHistoryResponseListener
    {
        val readyToSellStuff = RaiiProp(Opt.none<ReadyToSellStuff>())
        val readyToConsumeStuff = RaiiProp(Opt.none<ReadyToConsumeStuff>())

        init
        {
            postOnUiThread()
            {
                readyToSellStuff.value = {Opt.some(ReadyToSellStuff(this))}

                // execute pending actions
                connected.opened.billingClientFacade.doWhenListingsLoaded {}
            }
            connected.billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP,this)
        }

        override fun close()
        {
            readyToConsumeStuff.value = {Opt.none()}
            readyToSellStuff.value = {Opt.none()}
        }

        override fun onPurchaseHistoryResponse(responseCode:Int,purchasesList:MutableList<Purchase>?)
        {
            if (connected.offers.value.invoke().opt != this) return
            when(responseCode)
            {
                // connection failed
                BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED,
                BillingClient.BillingResponse.SERVICE_DISCONNECTED,
                BillingClient.BillingResponse.USER_CANCELED,
                BillingClient.BillingResponse.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponse.BILLING_UNAVAILABLE,
                BillingClient.BillingResponse.ITEM_UNAVAILABLE,
                BillingClient.BillingResponse.DEVELOPER_ERROR,
                BillingClient.BillingResponse.ERROR,
                BillingClient.BillingResponse.ITEM_ALREADY_OWNED,
                BillingClient.BillingResponse.ITEM_NOT_OWNED -> Unit

                // offers received
                BillingClient.BillingResponse.OK ->
                {
                    readyToConsumeStuff.value = {Opt.some(ReadyToConsumeStuff(this,purchasesList?:listOf()))}
                }
                else -> throw IllegalArgumentException("unknown billingResponseCode: $responseCode")
            }.forceExhaustiveWhen
        }
    }

    // await calls to buy stuff
    private class ReadyToSellStuff(
            val offers:Offers)
        :Closeable,IapListings
    {
        init
        {
            offers.connected.opened.billingClientFacade._onPurchaseStateChanged.value += 1
        }

        fun buy(activity:Activity,sku:String):SkuDetails?
        {
            if (offers.readyToSellStuff.value.invoke().opt != this) return null

            // get the thing we want to buy
            val thingToBuy = offers.offers.find {it.sku == sku}?:return null

            // buy it
            offers.connected.billingClient.launchBillingFlow(
                    activity,
                    BillingFlowParams.newBuilder()
                            .setSkuDetails(thingToBuy)
                            .build())

            // return the thing we bought
            return thingToBuy
        }

        override fun details(sku:String):SkuDetails?
        {
            return offers.offers.firstOrNull {it.sku == sku}
        }

        override fun isPurchased(sku:String):Boolean
        {
            return offers.connected.billingClient
                    .queryPurchases(BillingClient.SkuType.INAPP)
                    .run {purchasesList?:listOf()}
                    .filter {it.sku == sku}
                    .let {it.firstOrNull() != null}
        }

        override fun close() = Unit
    }

    // await calls to consume stuff
    private class ReadyToConsumeStuff(
            val offers:Offers,
            val purchaseHistory:List<Purchase>)
        :Closeable
    {
        fun consume(sku:String)
        {
            if (offers.readyToConsumeStuff.value.invoke().opt != this) return

            // get the thing we want to buy
            val boughtThings = purchaseHistory.filter {it.sku == sku}

            // consume it
            boughtThings.forEach()
            {
                boughtThing ->
                offers.connected.billingClient.consumeAsync(boughtThing.purchaseToken,offers.connected.opened)
            }
        }

        override fun close() = Unit
    }
}
