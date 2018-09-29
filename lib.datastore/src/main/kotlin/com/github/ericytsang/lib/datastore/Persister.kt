package com.github.ericytsang.lib.datastore

interface RoPersister<in Pk:Any,Entity:Any>
{
    fun select(pk:Pk):Entity?
}

interface RwPersister<in Pk:Any,in Values:Any,Entity:Any>:RoPersister<Pk,Entity>
{
    fun insert(values:Values):Entity
    fun update(pk:Pk,values:Values):Entity
    fun delete(pk:Pk)
}
