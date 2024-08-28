/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalKotest::class)
@file:UseSerializers(UUIDSerializer::class)

package com.google.firebase.dataconnect

import com.google.firebase.dataconnect.serializers.UUIDSerializer
import com.google.firebase.dataconnect.testutil.DataConnectIntegrationTestBase
import com.google.firebase.dataconnect.testutil.EdgeCases
import com.google.firebase.dataconnect.testutil.anyScalar
import com.google.firebase.dataconnect.testutil.anyScalarNotMatching
import com.google.firebase.dataconnect.testutil.expectedAnyScalarRoundTripValue
import io.kotest.assertions.asClue
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.property.Arb
import io.kotest.property.EdgeConfig
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.filterIsInstance
import io.kotest.property.arbitrary.next
import io.kotest.property.assume
import io.kotest.property.checkAll
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.serializer
import org.junit.Test

class AnyScalarIntegrationTest : DataConnectIntegrationTestBase() {

  private val dataConnect: FirebaseDataConnect by lazy {
    val connectorConfig = testConnectorConfig.copy(connector = "demo")
    dataConnectFactory.newInstance(connectorConfig)
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for inserting into and querying this table:
  // type NonNullableAnyScalar @table { value: Any!, tag: String }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun nonNullableAnyScalar_MutationVariableEdgeCases() = runTest {
    assertSoftly {
      for (value in EdgeCases.anyScalars.filterNotNull()) {
        withClue("value=$value") {
          verifyAnyScalarRoundTrip(
            value,
            insertMutationName = "NonNullableAnyScalarInsert",
            getByKeyQueryName = "NonNullableAnyScalarGetByKey",
          )
        }
      }
    }
  }

  @Test
  fun nonNullableAnyScalar_QueryVariableEdgeCases() = runTest {
    assertSoftly {
      for (value in EdgeCases.anyScalars.filterNotNull()) {
        val otherValues = List(2) { Arb.anyScalarNotMatching(value).filter { it !== null }.next() }
        withClue("value=$value otherValues=$otherValues") {
          verifyAnyScalarQueryVariable(
            value,
            otherValues[0],
            otherValues[1],
            insert3MutationName = "NonNullableAnyScalarInsert3",
            getAllByTagAndValueQueryName = "NonNullableAnyScalarGetAllByTagAndValue"
          )
        }
      }
    }
  }

  @Test
  fun nonNullableAnyScalar_MutationVariableNormalCases() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar()) { value ->
      assume(value !== null)
      verifyAnyScalarRoundTrip(
        value,
        insertMutationName = "NonNullableAnyScalarInsert",
        getByKeyQueryName = "NonNullableAnyScalarGetByKey",
      )
    }
  }

  @Test
  fun nonNullableAnyScalar_QueryVariableNormalCases() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar()) { value ->
      assume(value !== null)
      val otherValues = List(2) { Arb.anyScalarNotMatching(value).filter { it !== null }.next() }
      verifyAnyScalarQueryVariable(
        value,
        otherValues[0],
        otherValues[1],
        insert3MutationName = "NonNullableAnyScalarInsert3",
        getAllByTagAndValueQueryName = "NonNullableAnyScalarGetAllByTagAndValue"
      )
    }
  }

  @Test
  fun nonNullableAnyScalar_MutationVariableFailsIfMissing() = runTest {
    verifyMutationWithMissingAnyVariableFails("NonNullableAnyScalarInsert")
  }

  @Test
  fun nonNullableAnyScalar_QueryVariableFailsIfMissing() = runTest {
    verifyQueryWithMissingAnyVariableFails("NonNullableAnyScalarGetAllByTagAndValue")
  }

  @Test
  fun nonNullableAnyScalar_MutationVariableFailsIfNull() = runTest {
    verifyMutationWithNullVariableValueFails("NonNullableAnyScalarInsert")
  }

  @Test
  fun nonNullableAnyScalar_QueryVariableFailsIfNull() = runTest {
    verifyQueryWithNullVariableValueFails("NonNullableAnyScalarGetAllByTagAndValue")
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for inserting into and querying this table:
  // type NullableAnyScalar @table { value: Any, tag: String }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun nullableAnyScalar_MutationVariableEdgeCases() = runTest {
    assertSoftly {
      for (value in EdgeCases.anyScalars) {
        withClue("value=$value") {
          verifyAnyScalarRoundTrip(
            value,
            insertMutationName = "NullableAnyScalarInsert",
            getByKeyQueryName = "NullableAnyScalarGetByKey",
          )
        }
      }
    }
  }

  @Test
  fun nullableAnyScalar_QueryVariableEdgeCases() = runTest {
    assertSoftly {
      for (value in EdgeCases.anyScalars) {
        val otherValues = List(2) { Arb.anyScalarNotMatching(value).next() }
        withClue("value=$value otherValues=$otherValues") {
          verifyAnyScalarQueryVariable(
            value,
            otherValues[0],
            otherValues[1],
            insert3MutationName = "NullableAnyScalarInsert3",
            getAllByTagAndValueQueryName = "NullableAnyScalarGetAllByTagAndValue"
          )
        }
      }
    }
  }

  @Test
  fun nullableAnyScalar_MutationVariableNormalCases() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar()) { value ->
      verifyAnyScalarRoundTrip(
        value,
        insertMutationName = "NullableAnyScalarInsert",
        getByKeyQueryName = "NullableAnyScalarGetByKey",
      )
    }
  }

  @Test
  fun nullableAnyScalar_QueryVariableNormalCases() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar()) { value ->
      val otherValues = List(2) { Arb.anyScalarNotMatching(value).next() }
      verifyAnyScalarQueryVariable(
        value,
        otherValues[0],
        otherValues[1],
        insert3MutationName = "NullableAnyScalarInsert3",
        getAllByTagAndValueQueryName = "NullableAnyScalarGetAllByTagAndValue"
      )
    }
  }

  @Test
  fun nullableAnyScalar_MutationVariableSucceedsIfMissing() = runTest {
    val key = executeInsertMutation("InsertIntoNullableAnyScalar", EmptyVariables)
    verifyQueryResult2("GetFromNullableAnyScalarByKey", key, null)
  }

  @Test
  fun nullableAnyScalar_QueryVariableSucceedsIfMissing() = runTest {
    // TODO: factor this out to a reuable method
    val value = Arb.anyScalar().next()
    val key = executeInsertMutation("InsertIntoNullableAnyScalar", value)
    val id = UUIDSerializer.serialize(key.id)

    val queryRef =
      dataConnect.query(
        operationName = "GetFromNullableAnyScalarByIdAndValue",
        variables = IdQueryVariables(key.id),
        DataConnectUntypedData,
        serializer(),
      )
    val queryResult = queryRef.execute()
    queryResult.data.asClue {
      it.data.shouldNotBeNull()
      it.data shouldBe
        mapOf(
          "items" to listOf(mapOf("id" to id, "value" to expectedAnyScalarRoundTripValue(value)))
        )
      it.errors.shouldBeEmpty()
    }
  }

  @Test
  fun nullableAnyScalar_MutationVariableSucceedsIfNull() = runTest {
    val key = executeInsertMutation("InsertIntoNullableAnyScalar", null)
    verifyQueryResult2("GetFromNullableAnyScalarByKey", key, null)
  }

  @Test
  fun nullableAnyScalar_QueryVariableSucceedsIfNull() = runTest {
    // TODO: factor this out to a reuable method
    val key = executeInsertMutation("InsertIntoNullableAnyScalar", null)
    val id = UUIDSerializer.serialize(key.id)

    val queryVariables = DataConnectUntypedVariables("id" to id, "value" to null)
    val queryRef =
      dataConnect.query(
        operationName = "GetFromNullableAnyScalarByIdAndValue",
        variables = queryVariables,
        DataConnectUntypedData,
        DataConnectUntypedVariables,
      )
    val queryResult = queryRef.execute()
    queryResult.data.asClue {
      it.data.shouldNotBeNull()
      it.data shouldBe mapOf("items" to listOf(mapOf("id" to id, "value" to null)))
      it.errors.shouldBeEmpty()
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for inserting into and querying this table:
  // type AnyScalarNullableListNullable @table { value: [Any] }
  // mutation InsertIntoAnyScalarNullableListNullable($value: [Any!]) { key: ... }
  // query GetFromAnyScalarNullableListNullableById($id: UUID!) { item: ... }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun nullableListOfNullableAnyEdgeCasesRoundTrip() = runTest {
    assertSoftly {
      for (value in EdgeCases.lists) {
        withClue("value=$value") {
          val key = executeInsertMutation("InsertIntoAnyScalarNullableListNullable", value)
          val expectedQueryResult = expectedAnyScalarRoundTripValue(value)
          verifyQueryResult("GetFromAnyScalarNullableListNullableById", key, expectedQueryResult)
        }
      }
    }
  }

  @Test
  fun nullableListOfNullableAnyNormalCasesRoundTrip() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar().filterIsInstance<Any?, List<*>>()) { value ->
      val key = executeInsertMutation("InsertIntoAnyScalarNullableListNullable", value)
      val expectedQueryResult = expectedAnyScalarRoundTripValue(value)
      verifyQueryResult("GetFromAnyScalarNullableListNullableById", key, expectedQueryResult)
    }
  }

  @Test
  fun mutationMissingNullableListOfNullableAnyVariableShouldUseNull() = runTest {
    val key = executeInsertMutation("InsertIntoAnyScalarNullableListNullable", EmptyVariables)
    verifyQueryResult("GetFromAnyScalarNullableListNullableById", key, null)
  }

  @Test
  fun mutationNullForNullableListOfNullableAnyVariableShouldBeSetToNull() = runTest {
    val key = executeInsertMutation("InsertIntoAnyScalarNullableListNullable", null)
    verifyQueryResult("GetFromAnyScalarNullableListNullableById", key, null)
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for inserting into and querying this table:
  // type AnyScalarNonNullableListOfNullable @table { value: [Any]! }
  // mutation InsertIntoAnyScalarNonNullableListOfNullable($value: [Any!]!) { key: ... }
  // query GetFromAnyScalarNonNullableListOfNullableById($id: UUID!) { item: ... }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun nonNullableListOfNullableAnyEdgeCasesRoundTrip() = runTest {
    assertSoftly {
      for (value in EdgeCases.lists) {
        withClue("value=$value") {
          val key = executeInsertMutation("InsertIntoAnyScalarNonNullableListOfNullable", value)
          val expectedQueryResult = expectedAnyScalarRoundTripValue(value)
          verifyQueryResult(
            "GetFromAnyScalarNonNullableListOfNullableById",
            key,
            expectedQueryResult
          )
        }
      }
    }
  }

  @Test
  fun nonNullableListOfNullableAnyNormalCasesRoundTrip() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar().filterIsInstance<Any?, List<*>>()) { value ->
      val key = executeInsertMutation("InsertIntoAnyScalarNonNullableListOfNullable", value)
      val expectedQueryResult = expectedAnyScalarRoundTripValue(value)
      verifyQueryResult("GetFromAnyScalarNonNullableListOfNullableById", key, expectedQueryResult)
    }
  }

  @Test
  fun mutationMissingNonNullableListOfNullableAnyVariableShouldFail() = runTest {
    verifyMutationWithMissingAnyVariableFails("InsertIntoAnyScalarNonNullableListOfNullable")
  }

  @Test
  fun mutationNullValueForNonNullableListOfNullableAnyVariableShouldFail() = runTest {
    verifyMutationWithNullVariableValueFails("InsertIntoAnyScalarNonNullableListOfNullable")
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for inserting into and querying this table:
  // type AnyScalarNullableListOfNonNullable @table { value: [Any!] }
  // mutation InsertIntoAnyScalarNullableListOfNonNullable($value: [Any!]) { key: ... }
  // query GetFromAnyScalarNullableListOfNonNullableById($id: UUID!) { item: ... }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun nullableListOfNonNullableAnyEdgeCasesRoundTrip() = runTest {
    assertSoftly {
      for (value in EdgeCases.lists) {
        withClue("value=$value") {
          val key = executeInsertMutation("InsertIntoAnyScalarNullableListOfNonNullable", value)
          val expectedQueryResult = expectedAnyScalarRoundTripValue(value)
          verifyQueryResult(
            "GetFromAnyScalarNullableListOfNonNullableById",
            key,
            expectedQueryResult
          )
        }
      }
    }
  }

  @Test
  fun nullableListOfNonNullableAnyNormalCasesRoundTrip() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar().filterIsInstance<Any?, List<*>>()) { value ->
      val key = executeInsertMutation("InsertIntoAnyScalarNullableListOfNonNullable", value)
      val expectedQueryResult = expectedAnyScalarRoundTripValue(value)
      verifyQueryResult("GetFromAnyScalarNullableListOfNonNullableById", key, expectedQueryResult)
    }
  }

  @Test
  fun mutationMissingNullableListOfNonNullableAnyVariableShouldUseNull() = runTest {
    val key = executeInsertMutation("InsertIntoAnyScalarNullableListOfNonNullable", EmptyVariables)
    verifyQueryResult("GetFromAnyScalarNullableListOfNonNullableById", key, null)
  }

  @Test
  fun mutationNullForNullableListOfNonNullableAnyVariableShouldBeSetToNull() = runTest {
    val key = executeInsertMutation("InsertIntoAnyScalarNullableListOfNonNullable", null)
    verifyQueryResult("GetFromAnyScalarNullableListOfNonNullableById", key, null)
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for inserting into and querying this table:
  // type AnyScalarNonNullableListOfNonNullable @table { value: [Any!]! }
  // mutation InsertIntoAnyScalarNonNullableListOfNonNullable($value: [Any!]!) { key: ... }
  // query GetFromAnyScalarNonNullableListOfNonNullableById($id: UUID!) { item: ... }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun nonNullableListOfNonNullableAnyEdgeCasesRoundTrip() = runTest {
    assertSoftly {
      for (value in EdgeCases.lists) {
        withClue("value=$value") {
          val key = executeInsertMutation("InsertIntoAnyScalarNonNullableListOfNonNullable", value)
          val expectedQueryResult = expectedAnyScalarRoundTripValue(value)
          verifyQueryResult(
            "GetFromAnyScalarNonNullableListOfNonNullableById",
            key,
            expectedQueryResult
          )
        }
      }
    }
  }

  @Test
  fun nonNullableListOfNonNullableAnyNormalCasesRoundTrip() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar().filterIsInstance<Any?, List<*>>()) { value ->
      val key = executeInsertMutation("InsertIntoAnyScalarNonNullableListOfNonNullable", value)
      val expectedQueryResult = expectedAnyScalarRoundTripValue(value)
      verifyQueryResult(
        "GetFromAnyScalarNonNullableListOfNonNullableById",
        key,
        expectedQueryResult
      )
    }
  }

  @Test
  fun mutationMissingNonNullableListOfNonNullableAnyVariableShouldFail() = runTest {
    verifyMutationWithMissingAnyVariableFails("InsertIntoAnyScalarNonNullableListOfNonNullable")
  }

  @Test
  fun mutationNullForNonNullableListOfNonNullableAnyVariableShouldFail() = runTest {
    verifyMutationWithNullVariableValueFails("InsertIntoAnyScalarNonNullableListOfNonNullable")
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // End of tests; everything below is helper functions and classes.
  //////////////////////////////////////////////////////////////////////////////////////////////////

  object EmptyVariables

  /**
   * Verifies that a value used as an `Any` scalar specified as a variable to a mutation is handled
   * correctly. This is done by specifying the `Any` scalar value as a variable to a mutation that
   * inserts a row into a table, followed by querying that row by its key to ensure that an equal
   * `Any` value comes back from the query.
   *
   * @param value The value of the `Any` scalar to use; must be `null`, a [Boolean], [String],
   * [Double], or a [Map], or [List] composed of these types.
   * @param insertMutationName The operation name of a GraphQL mutation that takes a single variable
   * named "value" of type `Any` or `[Any]`, with any nullability; this mutation must insert a row
   * into a table and return a key for that row, where the key is a single "id" of type `UUID`.
   * @param getByKeyQueryName The operation name of a GraphQL query that takes a single variable
   * named "key" whose value is the key type returned from the `insertMutationName` mutation; its
   * selection set must have a single field named "item" whose value is the `Any` value specified to
   * the `insertMutationName` mutation.
   */
  private suspend fun verifyAnyScalarRoundTrip(
    value: Any?,
    insertMutationName: String,
    getByKeyQueryName: String,
  ) {
    val key = executeInsertMutation(insertMutationName, value)
    val expectedQueryResult = expectedAnyScalarRoundTripValue(value)
    verifyQueryResult2(getByKeyQueryName, key, expectedQueryResult)
  }

  private suspend fun verifyAnyScalarQueryVariable(
    value: Any?,
    value2: Any?,
    value3: Any?,
    insert3MutationName: String,
    getAllByTagAndValueQueryName: String,
  ) {
    require(value != value2)
    require(value != value3)
    require(expectedAnyScalarRoundTripValue(value) != expectedAnyScalarRoundTripValue(value2))
    require(expectedAnyScalarRoundTripValue(value) != expectedAnyScalarRoundTripValue(value3))

    @Serializable
    data class InsertData(val key1: TestTableKey, val key2: TestTableKey, val key3: TestTableKey)
    val tag = UUID.randomUUID().toString()
    val insert3MutationVariables =
      mapOf(
        "tag" to tag,
        "value1" to value,
        "value2" to value2,
        "value3" to value3,
      )
    val mutationRef =
      mutationRefForVariables(
        insert3MutationName,
        insert3MutationVariables,
        serializer<InsertData>()
      )
    val mutationResult = mutationRef.execute()
    val key = mutationResult.data.key1

    @Serializable data class QueryItem(val id: UUID)
    @Serializable data class QueryData(val items: List<QueryItem>)
    val queryVariables = mapOf("tag" to tag, "value" to value)
    val queryRef =
      queryRefForVariables(getAllByTagAndValueQueryName, queryVariables, serializer<QueryData>())
    val queryResult = queryRef.execute()
    queryResult.data.asClue { it.items.map { it.id } shouldContainExactly listOf(key.id) }
  }

  private inline fun <reified Data> mutationRefForVariables(
    operationName: String,
    variables: Map<String, Any?>,
    dataDeserializer: DeserializationStrategy<Data>,
  ): MutationRef<Data, DataConnectUntypedVariables> =
    dataConnect.mutation(
      operationName = operationName,
      variables = DataConnectUntypedVariables(variables),
      dataDeserializer,
      DataConnectUntypedVariables,
    )

  private inline fun <reified Data> queryRefForVariables(
    operationName: String,
    variables: Map<String, Any?>,
    dataDeserializer: DeserializationStrategy<Data>,
  ): QueryRef<Data, DataConnectUntypedVariables> =
    dataConnect.query(
      operationName = operationName,
      variables = DataConnectUntypedVariables(variables),
      dataDeserializer,
      DataConnectUntypedVariables,
    )

  private inline fun <reified Data> mutationRefForVariable(
    operationName: String,
    variable: Any?,
    dataDeserializer: DeserializationStrategy<Data>,
  ): MutationRef<Data, DataConnectUntypedVariables> =
    mutationRefForVariables(operationName, mapOf("value" to variable), dataDeserializer)

  private inline fun <reified Data> queryRefForVariable(
    operationName: String,
    variable: Any?,
    dataDeserializer: DeserializationStrategy<Data>,
  ): QueryRef<Data, DataConnectUntypedVariables> =
    queryRefForVariables(operationName, mapOf("value" to variable), dataDeserializer)

  private suspend fun verifyMutationWithNullVariableValueFails(operationName: String) {
    val mutationRef = mutationRefForVariable(operationName, null, DataConnectUntypedData)
    mutationRef.verifyExecuteFailsDueToNullVariable()
  }

  private suspend fun verifyQueryWithNullVariableValueFails(operationName: String) {
    val queryRef = queryRefForVariable(operationName, null, DataConnectUntypedData)
    queryRef.verifyExecuteFailsDueToNullVariable()
  }

  private suspend fun OperationRef<DataConnectUntypedData, *>
    .verifyExecuteFailsDueToNullVariable() {
    val result = execute()
    result.data.asClue {
      it.data.shouldBeNull()
      it.errors.shouldHaveAtLeastSize(1)
      it.errors[0].message shouldContainIgnoringCase "\$value is null"
    }
  }

  private suspend fun verifyMutationWithMissingAnyVariableFails(operationName: String) {
    val variables: Map<String, Any?> = emptyMap()
    val mutationRef = mutationRefForVariables(operationName, variables, DataConnectUntypedData)
    mutationRef.verifyExecuteFailsDueToMissingVariable()
  }

  private suspend fun verifyQueryWithMissingAnyVariableFails(operationName: String) {
    val variables: Map<String, Any?> = emptyMap()
    val queryRef = queryRefForVariables(operationName, variables, DataConnectUntypedData)
    queryRef.verifyExecuteFailsDueToMissingVariable()
  }

  private suspend fun OperationRef<DataConnectUntypedData, *>
    .verifyExecuteFailsDueToMissingVariable() {
    val result = execute()
    result.data.asClue {
      it.data.shouldBeNull()
      it.errors.shouldHaveAtLeastSize(1)
      it.errors[0].message shouldContainIgnoringCase "\$value is missing"
    }
  }

  private suspend fun executeInsertMutation(
    operationName: String,
    variable: Any?,
  ): TestTableKey {
    val mutationRef =
      mutationRefForVariable<IdMutationData>(
        operationName,
        variable,
        dataDeserializer = serializer(),
      )
    return mutationRef.execute().data.key
  }

  private suspend fun executeInsertMutation(
    operationName: String,
    @Suppress("UNUSED_PARAMETER") variables: EmptyVariables,
  ): TestTableKey {
    val mutationRef =
      mutationRefForVariables<IdMutationData>(
        operationName,
        emptyMap(),
        dataDeserializer = serializer(),
      )
    return mutationRef.execute().data.key
  }

  private suspend fun verifyQueryResult(
    operationName: String,
    key: TestTableKey,
    expectedData: Any?
  ) {
    val queryRef =
      dataConnect.query(
        operationName = operationName,
        variables = IdQueryVariables(key.id),
        DataConnectUntypedData,
        serializer(),
      )
    val queryResult = queryRef.execute()
    queryResult.data.asClue {
      it.data.shouldNotBeNull()
      it.data shouldBe mapOf("item" to mapOf("value" to expectedData))
      it.errors.shouldBeEmpty()
    }
  }

  private suspend fun verifyQueryResult2(
    operationName: String,
    key: TestTableKey,
    expectedData: Any?
  ) {
    val queryRef =
      dataConnect.query(
        operationName = operationName,
        variables = QueryByKeyVariables(key),
        DataConnectUntypedData,
        serializer(),
      )
    val queryResult = queryRef.execute()
    queryResult.data.asClue {
      it.data.shouldNotBeNull()
      it.data shouldBe mapOf("item" to mapOf("value" to expectedData))
      it.errors.shouldBeEmpty()
    }
  }

  private suspend fun verifyAnyScalarQueryVariable(
    key: TestTableKey,
    queryByKeyAndValueOperationName: String,
    variableValue: Any?,
    expectedQueryFieldValue: Any?,
  ) {
    val id = UUIDSerializer.serialize(key.id)
    val variables = DataConnectUntypedVariables("id" to id, "value" to variableValue)
    val queryRef =
      dataConnect.query(
        operationName = queryByKeyAndValueOperationName,
        variables = variables,
        DataConnectUntypedData,
        DataConnectUntypedVariables,
      )
    val queryResult = queryRef.execute()
    queryResult.data.asClue {
      it.data.shouldNotBeNull()
      it.data shouldBe
        mapOf("items" to listOf(mapOf("id" to id, "value" to expectedQueryFieldValue)))
      it.errors.shouldBeEmpty()
    }
  }

  @Serializable data class TestTableKey(val id: UUID)

  @Serializable private data class IdMutationData(val key: TestTableKey)

  @Serializable private data class IdQueryVariables(val id: UUID)

  @Serializable private data class QueryByKeyVariables(val key: TestTableKey)

  private companion object {

    val normalCasePropTestConfig =
      PropTestConfig(iterations = 20, edgeConfig = EdgeConfig(edgecasesGenerationProbability = 0.0))
  }
}
