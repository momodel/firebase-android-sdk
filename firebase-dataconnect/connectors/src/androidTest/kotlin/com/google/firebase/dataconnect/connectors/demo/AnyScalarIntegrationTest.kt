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
import com.google.firebase.dataconnect.testutil.anyScalarNotMatching
import com.google.firebase.dataconnect.testutil.expectedAnyScalarRoundTripValue
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.property.Arb
import io.kotest.property.EdgeConfig
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.next
import io.kotest.property.assume
import io.kotest.property.checkAll
import java.util.UUID
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.Test

class AnyScalarIntegrationTest : DemoConnectorIntegrationTestBase() {

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for inserting into and querying this table:
  // type NonNullableAnyScalar @table { value: Any!, tag: String }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun nonNullableAnyScalar_MutationVariableEdgeCases() = runTest {
    assertSoftly {
      for (value in EdgeCases.anyScalars.filterNotNull()) {
        withClue("value=$value") { verifyNonNullableAnyScalarRoundTrip(value) }
      }
    }
  }

  @Test
  fun nonNullableAnyScalar_QueryVariableEdgeCases() = runTest {
    assertSoftly {
      for (value in EdgeCases.anyScalars.filterNotNull()) {
        val otherValues = List(2) { Arb.anyScalarNotMatching(value).filter { it !== null }.next() }
        withClue("value=$value otherValues=$otherValues") {
          verifyNonNullableAnyScalarQueryVariable(value, otherValues[0], otherValues[1])
        }
      }
    }
  }

  @Test
  fun nonNullableAnyScalar_MutationVariableNormalCases() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar()) { value ->
      assume(value !== null)
      verifyNonNullableAnyScalarRoundTrip(value)
    }
  }

  @Test
  fun nonNullableAnyScalar_QueryVariableNormalCases() = runTest {
    checkAll(normalCasePropTestConfig, Arb.anyScalar()) { value ->
      assume(value !== null)
      val otherValues = List(2) { Arb.anyScalarNotMatching(value).filter { it !== null }.next() }
      verifyNonNullableAnyScalarQueryVariable(value, otherValues[0], otherValues[1])
    }
  }

  @Test
  fun nonNullableAnyScalar_MutationFailsIfAnyVariableIsMissing() = runTest {
    connector.nonNullableAnyScalarInsert.verifyFailsWithMissingVariableValue()
  }

  @Test
  fun nonNullableAnyScalar_QueryFailsIfAnyVariableIsMissing() = runTest {
    connector.nonNullableAnyScalarGetAllByTagAndValue.verifyFailsWithMissingVariableValue()
  }

  @Test
  fun nonNullableAnyScalar_MutationFailsIfAnyVariableIsNull() = runTest {
    connector.nonNullableAnyScalarInsert.verifyFailsWithNullVariableValue()
  }

  @Test
  fun nonNullableAnyScalar_QueryFailsIfAnyVariableIsNull() = runTest {
    connector.nonNullableAnyScalarGetAllByTagAndValue.verifyFailsWithNullVariableValue()
  }

  private suspend fun verifyNonNullableAnyScalarRoundTrip(value: Any?) {
    val anyValue = AnyValue.fromAny(value)
    val expectedQueryResult = AnyValue.fromAny(expectedAnyScalarRoundTripValue(value))
    val key = connector.nonNullableAnyScalarInsert.execute(anyValue) {}.data.key

    val queryResult = connector.nonNullableAnyScalarGetByKey.execute(key)
    queryResult.data shouldBe
      NonNullableAnyScalarGetByKeyQuery.Data(
        NonNullableAnyScalarGetByKeyQuery.Data.Item(expectedQueryResult)
      )
  }

  private suspend fun verifyNonNullableAnyScalarQueryVariable(
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
      connector.nonNullableAnyScalarInsert3
        .execute(anyValue, anyValue2, anyValue3) { this.tag = tag }
        .data
        .key1

    val queryResult =
      connector.nonNullableAnyScalarGetAllByTagAndValue.execute(anyValue) { this.tag = tag }
    queryResult.data shouldBe
      NonNullableAnyScalarGetAllByTagAndValueQuery.Data(
        listOf(NonNullableAnyScalarGetAllByTagAndValueQuery.Data.ItemsItem(key.id))
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
