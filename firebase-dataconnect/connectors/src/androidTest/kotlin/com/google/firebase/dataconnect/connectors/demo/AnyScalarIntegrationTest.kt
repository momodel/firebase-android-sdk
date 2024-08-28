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

package com.google.firebase.dataconnect.connectors.demo

import com.google.firebase.dataconnect.AnyValue
import com.google.firebase.dataconnect.DataConnectException
import com.google.firebase.dataconnect.OperationRef
import com.google.firebase.dataconnect.connectors.demo.testutil.DemoConnectorIntegrationTestBase
import com.google.firebase.dataconnect.generated.GeneratedMutation
import com.google.firebase.dataconnect.generated.GeneratedQuery
import com.google.firebase.dataconnect.testutil.EdgeCases
import com.google.firebase.dataconnect.testutil.anyScalar
import com.google.firebase.dataconnect.testutil.expectedAnyScalarRoundTripValue
import com.google.firebase.dataconnect.testutil.filterNotAnyScalarMatching
import com.google.firebase.dataconnect.testutil.filterNotIncludesAllMatchingAnyScalars
import com.google.firebase.dataconnect.testutil.filterNotNull
import io.kotest.assertions.asClue
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.property.Arb
import io.kotest.property.EdgeConfig
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.filterIsInstance
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.orNull
import io.kotest.property.checkAll
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.Test

class AnyScalarIntegrationTest : DemoConnectorIntegrationTestBase() {

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for inserting into and querying this table:
  // type AnyScalarNonNullable @table { value: Any!, tag: String, position: Int }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun anyScalarNonNullable_MutationVariableEdgeCases() = runTest {
    assertSoftly {
      for (value in EdgeCases.anyScalars.filterNotNull()) {
        withClue("value=$value") { verifyAnyScalarNonNullableRoundTrip(value) }
      }
    }
  }

  @Test
  fun anyScalarNonNullable_QueryVariableEdgeCases() = runTest {
    assertSoftly {
      for (value in EdgeCases.anyScalars.filterNotNull()) {
        val otherValues = Arb.anyScalar().filterNotNull().filterNotAnyScalarMatching(value)
        withClue("value=$value otherValues=$otherValues") {
          verifyAnyScalarNonNullableQueryVariable(value, otherValues.next(), otherValues.next())
        }
      }
    }
  }

  @Test
  fun anyScalarNonNullable_MutationVariableNormalCases() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar().filterNotNull()) { value ->
      verifyAnyScalarNonNullableRoundTrip(value)
    }
  }

  @Test
  fun anyScalarNonNullable_QueryVariableNormalCases() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar().filterNotNull()) { value ->
      val otherValues = Arb.anyScalar().filterNotNull().filterNotAnyScalarMatching(value)
      verifyAnyScalarNonNullableQueryVariable(value, otherValues.next(), otherValues.next())
    }
  }

  @Test
  fun anyScalarNonNullable_MutationFailsIfAnyVariableIsMissing() = runTest {
    connector.anyScalarNonNullableInsert.verifyFailsWithMissingVariableValue()
  }

  @Test
  fun anyScalarNonNullable_QueryFailsIfAnyVariableIsMissing() = runTest {
    connector.anyScalarNonNullableGetAllByTagAndValue.verifyFailsWithMissingVariableValue()
  }

  @Test
  fun anyScalarNonNullable_MutationFailsIfAnyVariableIsNull() = runTest {
    connector.anyScalarNonNullableInsert.verifyFailsWithNullVariableValue()
  }

  @Test
  fun anyScalarNonNullable_QueryFailsIfAnyVariableIsNull() = runTest {
    connector.anyScalarNonNullableGetAllByTagAndValue.verifyFailsWithNullVariableValue()
  }

  private suspend fun verifyAnyScalarNonNullableRoundTrip(value: Any) {
    val anyValue = AnyValue.fromAny(value)
    val expectedQueryResult = AnyValue.fromAny(expectedAnyScalarRoundTripValue(value))
    val key = connector.anyScalarNonNullableInsert.execute(anyValue) {}.data.key

    val queryResult = connector.anyScalarNonNullableGetByKey.execute(key)
    queryResult.data shouldBe
      AnyScalarNonNullableGetByKeyQuery.Data(
        AnyScalarNonNullableGetByKeyQuery.Data.Item(expectedQueryResult)
      )
  }

  private suspend fun verifyAnyScalarNonNullableQueryVariable(
    value: Any,
    value2: Any,
    value3: Any,
  ) {
    require(value != value2)
    require(value != value3)
    require(expectedAnyScalarRoundTripValue(value) != expectedAnyScalarRoundTripValue(value2))
    require(expectedAnyScalarRoundTripValue(value) != expectedAnyScalarRoundTripValue(value3))

    val tag = UUID.randomUUID().toString()
    val anyValue = AnyValue.fromAny(value)
    val anyValue2 = AnyValue.fromAny(value2)
    val anyValue3 = AnyValue.fromAny(value3)
    val key =
      connector.anyScalarNonNullableInsert3
        .execute(anyValue, anyValue2, anyValue3) { this.tag = tag }
        .data
        .key1

    val queryResult =
      connector.anyScalarNonNullableGetAllByTagAndValue.execute(anyValue) { this.tag = tag }
    queryResult.data shouldBe
      AnyScalarNonNullableGetAllByTagAndValueQuery.Data(
        listOf(AnyScalarNonNullableGetAllByTagAndValueQuery.Data.ItemsItem(key.id))
      )
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for inserting into and querying this table:
  // type AnyScalarNullable @table { value: Any, tag: String, position: Int }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun anyScalarNullable_MutationVariableEdgeCases() = runTest {
    assertSoftly {
      for (value in EdgeCases.anyScalars) {
        withClue("value=$value") { verifyAnyScalarNullableRoundTrip(value) }
      }
    }
  }

  @Test
  fun anyScalarNullable_QueryVariableEdgeCases() = runTest {
    assertSoftly {
      for (value in EdgeCases.anyScalars) {
        val otherValues = Arb.anyScalar().filterNotAnyScalarMatching(value)
        withClue("value=$value otherValues=$otherValues") {
          verifyAnyScalarNullableQueryVariable(value, otherValues.next(), otherValues.next())
        }
      }
    }
  }

  @Test
  fun anyScalarNullable_MutationVariableNormalCases() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar()) { value ->
      verifyAnyScalarNullableRoundTrip(value)
    }
  }

  @Test
  fun anyScalarNullable_QueryVariableNormalCases() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar()) { value ->
      val otherValues = Arb.anyScalar().filterNotAnyScalarMatching(value)
      verifyAnyScalarNullableQueryVariable(value, otherValues.next(), otherValues.next())
    }
  }

  @Test
  fun anyScalarNullable_MutationSucceedsIfAnyVariableIsMissing() = runTest {
    val key = connector.anyScalarNullableInsert.execute {}.data.key
    val queryResult = connector.anyScalarNullableGetByKey.execute(key)
    queryResult.data.asClue { it.item?.value.shouldBeNull() }
  }

  @Test
  fun anyScalarNullable_QuerySucceedsIfAnyVariableIsMissing() = runTest {
    val values = Arb.anyScalar().map { AnyValue.fromAny(it) }
    val tag = UUID.randomUUID().toString()
    val keys =
      connector.anyScalarNullableInsert3
        .execute {
          this.tag = tag
          this.value1 = values.next()
          this.value2 = values.next()
          this.value3 = values.next()
        }
        .data

    val queryResult = connector.anyScalarNullableGetAllByTagAndValue.execute { this.tag = tag }
    queryResult.data.asClue {
      it shouldBe
        AnyScalarNullableGetAllByTagAndValueQuery.Data(
          listOf(
            AnyScalarNullableGetAllByTagAndValueQuery.Data.ItemsItem(keys.key1.id),
            AnyScalarNullableGetAllByTagAndValueQuery.Data.ItemsItem(keys.key2.id),
            AnyScalarNullableGetAllByTagAndValueQuery.Data.ItemsItem(keys.key3.id),
          )
        )
    }
  }

  @Test
  fun anyScalarNullable_MutationSucceedsIfAnyVariableIsNull() = runTest {
    val key = connector.anyScalarNullableInsert.execute { value = null }.data.key
    val queryResult = connector.anyScalarNullableGetByKey.execute(key)
    queryResult.data.asClue { it.item?.value.shouldBeNull() }
  }

  @Test
  fun anyScalarNullable_QuerySucceedsIfAnyVariableIsNull() = runTest {
    val values = Arb.anyScalar().filter { it !== null }.map { AnyValue.fromAny(it) }
    val tag = UUID.randomUUID().toString()
    val keys =
      connector.anyScalarNullableInsert3
        .execute {
          this.tag = tag
          this.value1 = null
          this.value2 = values.next()
          this.value3 = values.next()
        }
        .data

    val queryResult =
      connector.anyScalarNullableGetAllByTagAndValue.execute {
        this.tag = tag
        this.value = null
      }
    queryResult.data.asClue {
      it shouldBe
        AnyScalarNullableGetAllByTagAndValueQuery.Data(
          listOf(
            AnyScalarNullableGetAllByTagAndValueQuery.Data.ItemsItem(keys.key1.id),
          )
        )
    }
  }

  private suspend fun verifyAnyScalarNullableRoundTrip(value: Any?) {
    val anyValue = AnyValue.fromAny(value)
    val expectedQueryResult = AnyValue.fromAny(expectedAnyScalarRoundTripValue(value))
    val key = connector.anyScalarNullableInsert.execute { this.value = anyValue }.data.key

    val queryResult = connector.anyScalarNullableGetByKey.execute(key)
    queryResult.data shouldBe
      AnyScalarNullableGetByKeyQuery.Data(
        AnyScalarNullableGetByKeyQuery.Data.Item(expectedQueryResult)
      )
  }

  private suspend fun verifyAnyScalarNullableQueryVariable(
    value: Any?,
    value2: Any?,
    value3: Any?
  ) {
    require(value != value2)
    require(value != value3)
    require(expectedAnyScalarRoundTripValue(value) != expectedAnyScalarRoundTripValue(value2))
    require(expectedAnyScalarRoundTripValue(value) != expectedAnyScalarRoundTripValue(value3))

    val tag = UUID.randomUUID().toString()
    val anyValue = AnyValue.fromAny(value)
    val anyValue2 = AnyValue.fromAny(value2)
    val anyValue3 = AnyValue.fromAny(value3)
    val key =
      connector.anyScalarNullableInsert3
        .execute {
          this.tag = tag
          this.value1 = anyValue
          this.value2 = anyValue2
          this.value3 = anyValue3
        }
        .data
        .key1

    val queryResult =
      connector.anyScalarNullableGetAllByTagAndValue.execute {
        this.value = anyValue
        this.tag = tag
      }
    queryResult.data shouldBe
      AnyScalarNullableGetAllByTagAndValueQuery.Data(
        listOf(AnyScalarNullableGetAllByTagAndValueQuery.Data.ItemsItem(key.id))
      )
  }
  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for inserting into and querying this table:
  // type AnyScalarNullableListOfNullable @table { value: [Any], tag: String, position: Int }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun anyScalarNullableListOfNullable_MutationVariableEdgeCases() = runTest {
    assertSoftly {
      val edgeCases = EdgeCases.anyScalars.filterIsInstance<List<Any?>>().map { it.filterNotNull() }
      for (value in edgeCases) {
        withClue("value=$value") { verifyAnyScalarNullableListOfNullableRoundTrip(value) }
      }
    }
  }

  @Test
  fun anyScalarNullableListOfNullable_QueryVariableEdgeCases() = runTest {
    val edgeCases =
      EdgeCases.anyScalars
        .filterIsInstance<List<Any?>>()
        .map { it.filterNotNull() }
        .filter { it.isNotEmpty() }
    val otherValues =
      Arb.anyScalar().filterIsInstance<Any?, List<Any?>>().map { it.filterNotNull() }

    assertSoftly {
      for (value in edgeCases) {
        val curOtherValues =
          otherValues.filterNotIncludesAllMatchingAnyScalars(value).orNull(nullProbability = 0.1)
        withClue("value=$value") {
          verifyAnyScalarNullableListOfNullableQueryVariable(
            value,
            curOtherValues.next(),
            curOtherValues.next()
          )
        }
      }
    }
  }

  @Test
  fun anyScalarNullableListOfNullable_MutationVariableNormalCases() = runTest {
    val values = Arb.anyScalar().filterIsInstance<Any?, List<Any?>>().map { it.filterNotNull() }
    checkAll(normalCasePropTestConfig, values) { value ->
      verifyAnyScalarNullableListOfNullableRoundTrip(value)
    }
  }

  @Test
  fun anyScalarNullableListOfNullable_QueryVariableNormalCases() = runTest {
    val values =
      Arb.anyScalar()
        .filterIsInstance<Any?, List<Any?>>()
        .map { it.filterNotNull() }
        .filter { it.isNotEmpty() }
    val otherValues =
      Arb.anyScalar().filterIsInstance<Any?, List<Any?>>().map { it.filterNotNull() }

    checkAll(normalCasePropTestConfig, values) { value ->
      val curOtherValues =
        otherValues.filterNotIncludesAllMatchingAnyScalars(value).orNull(nullProbability = 0.1)
      verifyAnyScalarNullableListOfNullableQueryVariable(
        value,
        curOtherValues.next(),
        curOtherValues.next()
      )
    }
  }

  private suspend fun verifyAnyScalarNullableListOfNullableRoundTrip(value: List<Any>?) {
    val anyValue = value?.map { AnyValue.fromAny(it) }
    val expectedQueryResult = value?.map { AnyValue.fromAny(expectedAnyScalarRoundTripValue(it)) }
    val key =
      connector.anyScalarNullableListOfNullableInsert.execute { this.value = anyValue }.data.key

    val queryResult = connector.anyScalarNullableListOfNullableGetByKey.execute(key)
    queryResult.data shouldBe
      AnyScalarNullableListOfNullableGetByKeyQuery.Data(
        AnyScalarNullableListOfNullableGetByKeyQuery.Data.Item(expectedQueryResult)
      )
  }

  private suspend fun verifyAnyScalarNullableListOfNullableQueryVariable(
    value: List<Any>?,
    value2: List<Any>?,
    value3: List<Any>?,
  ) {
    require(value != value2)
    require(value != value3)
    // TODO: implement a check to ensure that value is not a subset of value2 and value3.

    val tag = UUID.randomUUID().toString()
    val anyValue = value?.map(AnyValue::fromAny)
    val anyValue2 = value2?.map(AnyValue::fromAny)
    val anyValue3 = value3?.map(AnyValue::fromAny)
    val key =
      connector.anyScalarNullableListOfNullableInsert3
        .execute {
          this.tag = tag
          this.value1 = anyValue
          this.value2 = anyValue2
          this.value3 = anyValue3
        }
        .data
        .key1

    val queryResult =
      connector.anyScalarNullableListOfNullableGetAllByTagAndValue.execute {
        this.value = anyValue
        this.tag = tag
      }
    queryResult.data shouldBe
      AnyScalarNullableListOfNullableGetAllByTagAndValueQuery.Data(
        listOf(AnyScalarNullableListOfNullableGetAllByTagAndValueQuery.Data.ItemsItem(key.id))
      )
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // End of tests; everything below is helper functions and classes.
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Serializable private data class VariablesWithNullValue(val value: String?)

  private companion object {

    @OptIn(ExperimentalKotest::class)
    val normalCasePropTestConfig =
      PropTestConfig(iterations = 20, edgeConfig = EdgeConfig(edgecasesGenerationProbability = 0.0))

    suspend fun GeneratedMutation<*, *, *>.verifyFailsWithMissingVariableValue() {
      val mutationRef =
        connector.dataConnect.mutation(
          operationName = operationName,
          variables = Unit,
          dataDeserializer = dataDeserializer,
          variablesSerializer = serializer<Unit>(),
        )
      mutationRef.verifyExecuteFailsDueToMissingVariable()
    }

    suspend fun GeneratedQuery<*, *, *>.verifyFailsWithMissingVariableValue() {
      val queryRef =
        connector.dataConnect.query(
          operationName = operationName,
          variables = Unit,
          dataDeserializer = dataDeserializer,
          variablesSerializer = serializer<Unit>(),
        )
      queryRef.verifyExecuteFailsDueToMissingVariable()
    }

    suspend fun OperationRef<*, *>.verifyExecuteFailsDueToMissingVariable() {
      val exception = shouldThrow<DataConnectException> { execute() }
      exception.message shouldContainIgnoringCase "\$value is missing"
    }

    suspend fun GeneratedMutation<*, *, *>.verifyFailsWithNullVariableValue() {
      val mutationRef =
        connector.dataConnect.mutation(
          operationName = operationName,
          variables = VariablesWithNullValue(null),
          dataDeserializer = dataDeserializer,
          variablesSerializer = serializer<VariablesWithNullValue>(),
        )

      mutationRef.verifyExecuteFailsDueToNullVariable()
    }

    suspend fun GeneratedQuery<*, *, *>.verifyFailsWithNullVariableValue() {
      val queryRef =
        connector.dataConnect.query(
          operationName = operationName,
          variables = VariablesWithNullValue(null),
          dataDeserializer = dataDeserializer,
          variablesSerializer = serializer<VariablesWithNullValue>(),
        )

      queryRef.verifyExecuteFailsDueToNullVariable()
    }

    suspend fun OperationRef<*, *>.verifyExecuteFailsDueToNullVariable() {
      val exception = shouldThrow<DataConnectException> { execute() }
      exception.message shouldContainIgnoringCase "\$value is null"
    }
  }
}
