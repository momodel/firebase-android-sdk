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
import com.google.firebase.dataconnect.testutil.expectedAnyScalarRoundTripValue
import com.google.firebase.dataconnect.testutil.filterNotAnyScalarMatching
import com.google.firebase.dataconnect.testutil.filterNotNull
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
  // type AnyScalarNonNullable @table { value: Any!, tag: String }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun anyScalarNonNullable_MutationVariableEdgeCases() = runTest {
    assertSoftly {
      for (value in EdgeCases.anyScalars.filterNotNull()) {
        withClue("value=$value") {
          verifyAnyScalarRoundTrip(
            value,
            insertMutationName = "AnyScalarNonNullableInsert",
            getByKeyQueryName = "AnyScalarNonNullableGetByKey",
          )
        }
      }
    }
  }

  @Test
  fun anyScalarNonNullable_QueryVariableEdgeCases() = runTest {
    assertSoftly {
      for (value in EdgeCases.anyScalars.filterNotNull()) {
        val otherValues = Arb.anyScalar().filterNotNull().filterNotAnyScalarMatching(value)
        withClue("value=$value otherValues=$otherValues") {
          verifyAnyScalarQueryVariable(
            value,
            otherValues.next(),
            otherValues.next(),
            insert3MutationName = "AnyScalarNonNullableInsert3",
            getAllByTagAndValueQueryName = "AnyScalarNonNullableGetAllByTagAndValue"
          )
        }
      }
    }
  }

  @Test
  fun anyScalarNonNullable_MutationVariableNormalCases() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar().filterNotNull()) { value ->
      verifyAnyScalarRoundTrip(
        value,
        insertMutationName = "AnyScalarNonNullableInsert",
        getByKeyQueryName = "AnyScalarNonNullableGetByKey",
      )
    }
  }

  @Test
  fun anyScalarNonNullable_QueryVariableNormalCases() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar().filterNotNull()) { value ->
      val otherValues = Arb.anyScalar().filterNotNull().filterNotAnyScalarMatching(value)
      verifyAnyScalarQueryVariable(
        value,
        otherValues.next(),
        otherValues.next(),
        insert3MutationName = "AnyScalarNonNullableInsert3",
        getAllByTagAndValueQueryName = "AnyScalarNonNullableGetAllByTagAndValue"
      )
    }
  }

  @Test
  fun anyScalarNonNullable_MutationFailsIfAnyVariableIsMissing() = runTest {
    verifyMutationWithMissingAnyVariableFails("AnyScalarNonNullableInsert")
  }

  @Test
  fun anyScalarNonNullable_QueryFailsIfAnyVariableIsMissing() = runTest {
    verifyQueryWithMissingAnyVariableFails("AnyScalarNonNullableGetAllByTagAndValue")
  }

  @Test
  fun anyScalarNonNullable_MutationFailsIfAnyVariableIsNull() = runTest {
    verifyMutationWithNullAnyVariableFails("AnyScalarNonNullableInsert")
  }

  @Test
  fun anyScalarNonNullable_QueryFailsIfAnyVariableIsNull() = runTest {
    verifyQueryWithNullAnyVariableFails("AnyScalarNonNullableGetAllByTagAndValue")
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for inserting into and querying this table:
  // type AnyScalarNullable @table { value: Any, tag: String }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun anyScalarNullable_MutationVariableEdgeCases() = runTest {
    assertSoftly {
      for (value in EdgeCases.anyScalars) {
        withClue("value=$value") {
          verifyAnyScalarRoundTrip(
            value,
            insertMutationName = "AnyScalarNullableInsert",
            getByKeyQueryName = "AnyScalarNullableGetByKey",
          )
        }
      }
    }
  }

  @Test
  fun anyScalarNullable_QueryVariableEdgeCases() = runTest {
    assertSoftly {
      for (value in EdgeCases.anyScalars) {
        val otherValues = Arb.anyScalar().filterNotAnyScalarMatching(value)
        withClue("value=$value otherValues=$otherValues") {
          verifyAnyScalarQueryVariable(
            value,
            otherValues.next(),
            otherValues.next(),
            insert3MutationName = "AnyScalarNullableInsert3",
            getAllByTagAndValueQueryName = "AnyScalarNullableGetAllByTagAndValue"
          )
        }
      }
    }
  }

  @Test
  fun anyScalarNullable_MutationVariableNormalCases() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar()) { value ->
      verifyAnyScalarRoundTrip(
        value,
        insertMutationName = "AnyScalarNullableInsert",
        getByKeyQueryName = "AnyScalarNullableGetByKey",
      )
    }
  }

  @Test
  fun anyScalarNullable_QueryVariableNormalCases() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar()) { value ->
      val otherValues = Arb.anyScalar().filterNotAnyScalarMatching(value)
      verifyAnyScalarQueryVariable(
        value,
        otherValues.next(),
        otherValues.next(),
        insert3MutationName = "AnyScalarNullableInsert3",
        getAllByTagAndValueQueryName = "AnyScalarNullableGetAllByTagAndValue"
      )
    }
  }

  @Test
  fun anyScalarNullable_MutationSucceedsIfAnyVariableIsMissing() = runTest {
    verifyMutationWithMissingAnyVariableSucceeds(
      insertMutationName = "AnyScalarNullableInsert",
      getByKeyQueryName = "AnyScalarNullableGetByKey",
    )
  }

  @Test
  fun anyScalarNullable_QuerySucceedsIfAnyVariableIsMissing() = runTest {
    // TODO: factor this out to a reusable method
    val values = Arb.anyScalar()
    val tag = UUID.randomUUID().toString()
    val keys =
      executeInsert3Mutation(
        "AnyScalarNullableInsert3",
        tag,
        values.next(),
        values.next(),
        values.next()
      )

    val queryRef =
      dataConnect.query(
        operationName = "AnyScalarNullableGetAllByTagAndValue",
        variables = DataConnectUntypedVariables("tag" to tag),
        dataDeserializer = DataConnectUntypedData,
        variablesSerializer = DataConnectUntypedVariables,
      )
    val queryResult = queryRef.execute()
    queryResult.data.asClue {
      it.data shouldBe
        mapOf(
          "items" to
            listOf(
              mapOf("id" to keys.key1.id),
              mapOf("id" to keys.key2.id),
              mapOf("id" to keys.key3.id)
            )
        )
      it.errors.shouldBeEmpty()
    }
  }

  @Test
  fun anyScalarNullable_MutationSucceedsIfAnyVariableIsNull() = runTest {
    verifyMutationWithNullAnyVariableSucceeds(
      insertMutationName = "AnyScalarNullableInsert",
      getByKeyQueryName = "AnyScalarNullableGetByKey",
    )
  }

  @Test
  fun anyScalarNullable_QuerySucceedsIfAnyVariableIsNull() = runTest {
    // TODO: factor this out to a reusable method
    val values = Arb.anyScalar().filter { it !== null }
    val tag = UUID.randomUUID().toString()
    val keys =
      executeInsert3Mutation("AnyScalarNullableInsert3", tag, null, values.next(), values.next())

    val queryRef =
      dataConnect.query(
        operationName = "AnyScalarNullableGetAllByTagAndValue",
        variables = DataConnectUntypedVariables("tag" to tag, "value" to null),
        dataDeserializer = DataConnectUntypedData,
        variablesSerializer = DataConnectUntypedVariables,
      )
    val queryResult = queryRef.execute()
    queryResult.data.asClue {
      it.data shouldBe mapOf("items" to listOf(mapOf("id" to keys.key1.id)))
      it.errors.shouldBeEmpty()
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for inserting into and querying this table:
  // type AnyScalarNullableListOfNullable @table { value: [Any] }
  // mutation InsertIntoAnyScalarNullableListOfNullable($value: [Any!]) { key: ... }
  // query GetFromAnyScalarNullableListOfNullableById($id: UUID!) { item: ... }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun nullableListOfNullableAnyEdgeCasesRoundTrip() = runTest {
    assertSoftly {
      for (value in EdgeCases.lists) {
        withClue("value=$value") {
          val key = executeInsertMutation("InsertIntoAnyScalarNullableListOfNullable", value)
          val expectedQueryResult = expectedAnyScalarRoundTripValue(value)
          verifyQueryResult("GetFromAnyScalarNullableListOfNullableById", key, expectedQueryResult)
        }
      }
    }
  }

  @Test
  fun nullableListOfNullableAnyNormalCasesRoundTrip() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar().filterIsInstance<Any?, List<*>>()) { value ->
      val key = executeInsertMutation("InsertIntoAnyScalarNullableListOfNullable", value)
      val expectedQueryResult = expectedAnyScalarRoundTripValue(value)
      verifyQueryResult("GetFromAnyScalarNullableListOfNullableById", key, expectedQueryResult)
    }
  }

  @Test
  fun mutationMissingNullableListOfNullableAnyVariableShouldUseNull() = runTest {
    val key = executeInsertMutation("InsertIntoAnyScalarNullableListOfNullable", EmptyVariables)
    verifyQueryResult("GetFromAnyScalarNullableListOfNullableById", key, null)
  }

  @Test
  fun mutationNullForNullableListOfNullableAnyVariableShouldBeSetToNull() = runTest {
    val key = executeInsertMutation("InsertIntoAnyScalarNullableListOfNullable", null)
    verifyQueryResult("GetFromAnyScalarNullableListOfNullableById", key, null)
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
    verifyMutationWithNullAnyVariableFails("InsertIntoAnyScalarNonNullableListOfNullable")
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
    verifyMutationWithNullAnyVariableFails("InsertIntoAnyScalarNonNullableListOfNonNullable")
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

  private suspend fun verifyMutationWithNullAnyVariableFails(operationName: String) {
    val mutationRef = mutationRefForVariable(operationName, null, DataConnectUntypedData)
    mutationRef.verifyExecuteFailsDueToNullVariable()
  }

  private suspend fun verifyQueryWithNullAnyVariableFails(operationName: String) {
    val queryRef = queryRefForVariable(operationName, null, DataConnectUntypedData)
    queryRef.verifyExecuteFailsDueToNullVariable()
  }

  private suspend fun verifyMutationWithNullAnyVariableSucceeds(
    insertMutationName: String,
    getByKeyQueryName: String,
  ) {
    val key = executeInsertMutation(insertMutationName, null)
    verifyQueryResult2(getByKeyQueryName, key, null)
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

  private suspend fun verifyMutationWithMissingAnyVariableSucceeds(
    insertMutationName: String,
    getByKeyQueryName: String,
  ) {
    val key = executeInsertMutation(insertMutationName, EmptyVariables)
    verifyQueryResult2(getByKeyQueryName, key, null)
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

  private suspend fun executeInsert3Mutation(
    operationName: String,
    tag: String,
    value1: Any?,
    value2: Any?,
    value3: Any?,
  ): Insert3MutationDataStrings {
    val mutationRef =
      mutationRefForVariables<Insert3MutationDataStrings>(
        operationName,
        variables = mapOf("tag" to tag, "value1" to value1, "value2" to value2, "value3" to value3),
        dataDeserializer = serializer(),
      )
    return mutationRef.execute().data
  }

  private suspend fun executeInsertMutation(
    operationName: String,
    variable: Any?,
  ): TestTableKey {
    val mutationRef =
      mutationRefForVariable<InsertMutationData>(
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
      mutationRefForVariables<InsertMutationData>(
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

  @Serializable data class TestTableKey(val id: UUID)
  @Serializable data class TestTableKeyString(val id: String)

  @Serializable private data class InsertMutationData(val key: TestTableKey)

  @Serializable
  private data class Insert3MutationDataStrings(
    val key1: TestTableKeyString,
    val key2: TestTableKeyString,
    val key3: TestTableKeyString
  )

  @Serializable private data class IdQueryVariables(val id: UUID)

  @Serializable private data class QueryByKeyVariables(val key: TestTableKey)

  private companion object {

    val normalCasePropTestConfig =
      PropTestConfig(iterations = 20, edgeConfig = EdgeConfig(edgecasesGenerationProbability = 0.0))
  }
}
