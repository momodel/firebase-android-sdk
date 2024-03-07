// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.dataconnect.testutil.schemas

import com.google.firebase.dataconnect.FirebaseDataConnect
import com.google.firebase.dataconnect.mutation
import com.google.firebase.dataconnect.query
import com.google.firebase.dataconnect.testutil.TestDataConnectFactory
import com.google.firebase.dataconnect.testutil.installEmulatorSchema
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

class AllTypesSchema(val dataConnect: FirebaseDataConnect) {

  init {
    dataConnect.config.connector.let {
      require(it == CONNECTOR) {
        "The given FirebaseDataConnect has connector=$it, but expected $CONNECTOR"
      }
    }
  }

  suspend fun installEmulatorSchema() {
    dataConnect.installEmulatorSchema("testing_graphql_schemas/alltypes")
  }

  @Serializable
  data class PrimitiveData(
    val id: String,
    val idFieldNullable: String?,
    val intField: Int,
    val intFieldNullable: Int?,
    // NOTE: GraphQL "Float" type is a "signed double-precision floating-point value", which is
    // equivalent to Java and Kotlin's `Double` type.
    val floatField: Double,
    val floatFieldNullable: Double?,
    val booleanField: Boolean,
    val booleanFieldNullable: Boolean?,
    val stringField: String,
    val stringFieldNullable: String?,
  )

  object CreatePrimitiveMutation {
    @Serializable data class Variables(val data: PrimitiveData)
  }

  fun createPrimitive(variables: CreatePrimitiveMutation.Variables) =
    dataConnect.mutation(
      operationName = "createPrimitive",
      variables = variables,
      dataDeserializer = serializer<Unit>(),
      variablesSerializer = serializer<CreatePrimitiveMutation.Variables>(),
    )

  object GetPrimitiveQuery {
    @Serializable data class Data(val primitive: PrimitiveData?)
    @Serializable data class Variables(val id: String)
  }

  fun getPrimitive(variables: GetPrimitiveQuery.Variables) =
    dataConnect.query(
      operationName = "getPrimitive",
      variables = variables,
      dataDeserializer = serializer<GetPrimitiveQuery.Data>(),
      variablesSerializer = serializer<GetPrimitiveQuery.Variables>(),
    )

  fun getPrimitive(id: String) = getPrimitive(GetPrimitiveQuery.Variables(id = id))

  @Serializable
  data class PrimitiveListData(
    val id: String,
    val idListNullable: List<String>?,
    val idListOfNullable: List<String?>,
    val intList: List<Int>,
    val intListNullable: List<Int>?,
    val intListOfNullable: List<Int?>,
    // NOTE: GraphQL "Float" type is a "signed double-precision floating-point value", which is
    // equivalent to Java and Kotlin's `Double` type.
    val floatList: List<Double>,
    val floatListNullable: List<Double>?,
    val floatListOfNullable: List<Double?>,
    val booleanList: List<Boolean>,
    val booleanListNullable: List<Boolean>?,
    val booleanListOfNullable: List<Boolean?>,
    val stringList: List<String>,
    val stringListNullable: List<String>?,
    val stringListOfNullable: List<String?>,
  )

  object CreatePrimitiveListMutation {
    @Serializable data class Variables(val data: PrimitiveListData)
  }

  fun createPrimitiveList(variables: CreatePrimitiveListMutation.Variables) =
    dataConnect.mutation(
      operationName = "createPrimitiveList",
      variables = variables,
      dataDeserializer = serializer<Unit>(),
      variablesSerializer = serializer<CreatePrimitiveListMutation.Variables>(),
    )

  object GetPrimitiveListQuery {
    @Serializable data class Data(val primitiveList: PrimitiveListData?)
    @Serializable data class Variables(val id: String)
  }

  fun getPrimitiveList(variables: GetPrimitiveListQuery.Variables) =
    dataConnect.query(
      operationName = "getPrimitiveList",
      variables = variables,
      dataDeserializer = serializer<GetPrimitiveListQuery.Data>(),
      variablesSerializer = serializer<GetPrimitiveListQuery.Variables>(),
    )

  fun getPrimitiveList(id: String) = getPrimitiveList(GetPrimitiveListQuery.Variables(id = id))

  object GetAllPrimitiveListsQuery {
    @Serializable data class Data(val primitiveLists: List<PrimitiveListData>)
  }

  val getAllPrimitiveLists
    get() =
      dataConnect.query(
        operationName = "getAllPrimitiveLists",
        dataDeserializer = serializer<GetAllPrimitiveListsQuery.Data>()
      )

  object CreateFarmerMutation {
    @Serializable data class Variables(val data: Farmer)

    @Serializable
    data class Farmer(
      val id: String,
      val name: String,
      val parentId: String?,
    )
  }

  fun createFarmer(variables: CreateFarmerMutation.Variables) =
    dataConnect.mutation(
      operationName = "createFarmer",
      variables = variables,
      dataDeserializer = serializer<Unit>(),
      variablesSerializer = serializer<CreateFarmerMutation.Variables>(),
    )

  fun createFarmer(id: String, name: String, parentId: String?) =
    createFarmer(
      CreateFarmerMutation.Variables(
        CreateFarmerMutation.Farmer(id = id, name = name, parentId = parentId)
      )
    )

  object CreateFarmMutation {
    @Serializable data class Variables(val data: Farm)
    @Serializable data class Farm(val id: String, val name: String, val farmerId: String?)
  }

  fun createFarm(variables: CreateFarmMutation.Variables) =
    dataConnect.mutation(
      operationName = "createFarm",
      variables = variables,
      dataDeserializer = serializer<Unit>(),
      variablesSerializer = serializer<CreateFarmMutation.Variables>(),
    )

  fun createFarm(id: String, name: String, farmerId: String?) =
    createFarm(
      CreateFarmMutation.Variables(
        CreateFarmMutation.Farm(id = id, name = name, farmerId = farmerId)
      )
    )

  object CreateAnimalMutation {
    @Serializable data class Variables(val data: Animal)

    @Serializable
    data class Animal(
      val id: String,
      val farmId: String,
      val name: String,
      val species: String,
      val age: Int?
    )
  }

  fun createAnimal(variables: CreateAnimalMutation.Variables) =
    dataConnect.mutation(
      operationName = "createAnimal",
      variables = variables,
      dataDeserializer = serializer<Unit>(),
      variablesSerializer = serializer<CreateAnimalMutation.Variables>(),
    )

  fun createAnimal(id: String, farmId: String, name: String, species: String, age: Int?) =
    createAnimal(
      CreateAnimalMutation.Variables(
        CreateAnimalMutation.Animal(
          id = id,
          farmId = farmId,
          name = name,
          species = species,
          age = age
        )
      )
    )

  object GetFarmQuery {
    @Serializable data class Data(val farm: Farm?)

    @Serializable
    data class Farm(
      val id: String,
      val name: String,
      val farmer: Farmer,
      val animals: List<Animal>
    )

    @Serializable data class Farmer(val id: String, val name: String, val parent: Parent?)

    @Serializable data class Parent(val id: String, val name: String, val parentId: String?)

    @Serializable
    data class Animal(val id: String, val name: String, val species: String, val age: Int?)

    @Serializable data class Variables(val id: String)
  }

  fun getFarm(variables: GetFarmQuery.Variables) =
    dataConnect.query(
      operationName = "getFarm",
      variables = variables,
      dataDeserializer = serializer<GetFarmQuery.Data>(),
      variablesSerializer = serializer<GetFarmQuery.Variables>(),
    )

  fun getFarm(id: String) = getFarm(GetFarmQuery.Variables(id = id))

  companion object {
    const val CONNECTOR = "ops"
  }
}

suspend fun TestDataConnectFactory.installAllTypesSchema() =
  AllTypesSchema(newInstance(connector = AllTypesSchema.CONNECTOR)).apply {
    installEmulatorSchema()
  }

fun LazyAllTypesSchema(dataConnectFactory: TestDataConnectFactory): Lazy<AllTypesSchema> = lazy {
  runBlocking { dataConnectFactory.installAllTypesSchema() }
}