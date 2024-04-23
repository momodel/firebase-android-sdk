// Copyright 2024 Google LLC
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

package com.google.firebase.dataconnect.connectors.demo

import com.google.common.truth.Truth.assertThat
import com.google.firebase.Timestamp
import com.google.firebase.dataconnect.*
import com.google.firebase.dataconnect.connectors.demo.testutil.DemoConnectorIntegrationTestBase
import com.google.firebase.dataconnect.testutil.randomAlphanumericString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.reflect.full.memberProperties
import kotlinx.coroutines.test.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class ScalarVariablesAndDataIntegrationTest : DemoConnectorIntegrationTestBase() {

  @Test
  fun stringVariants() = runTest {
    val id = randomAlphanumericString()

    connector.insertStringVariants.execute(
      id = id,
      nonNullWithNonEmptyValue = "some non-empty value for a *non*-nullable field",
      nonNullWithEmptyValue = "",
      nullableWithNullValue = null,
      nullableWithNonNullValue = "some non-empty value for a *nullable* field",
      nullableWithEmptyValue = "",
      emptyList = emptyList(),
      nonEmptyList = listOf("foo", "", "BAR")
    )

    val queryResult = connector.getStringVariantsById.execute(id)
    assertThat(queryResult.data.stringVariants)
      .isEqualTo(
        GetStringVariantsByIdQuery.Data.StringVariants(
          nonNullWithNonEmptyValue = "some non-empty value for a *non*-nullable field",
          nonNullWithEmptyValue = "",
          nullableWithNullValue = null,
          nullableWithNonNullValue = "some non-empty value for a *nullable* field",
          nullableWithEmptyValue = "",
          emptyList = emptyList(),
          nonEmptyList = listOf("foo", "", "BAR")
        )
      )
  }

  @Test
  @Ignore("Un-ignore this test once the emulator fixes its handling of Int64 (b/331596857)")
  fun int64Variants() = runTest {
    val id = randomAlphanumericString()

    connector.insertInt64variants.execute(
      id = id,
      nonNullWithZeroValue = 0,
      nonNullWithPositiveValue = 4242424242424242,
      nonNullWithNegativeValue = -4242424242424242,
      nonNullWithMaxValue = Long.MAX_VALUE,
      nonNullWithMinValue = Long.MIN_VALUE,
      nullableWithNullValue = null,
      nullableWithZeroValue = 0,
      nullableWithPositiveValue = 2424242424242424,
      nullableWithNegativeValue = -2424242424242424,
      nullableWithMaxValue = Long.MAX_VALUE,
      nullableWithMinValue = Long.MIN_VALUE,
      emptyList = emptyList(),
      nonEmptyList = listOf(0, -1, 1, 99, -99, Long.MIN_VALUE, Long.MAX_VALUE)
    )

    val queryResult = connector.getInt64variantsById.execute(id)
    assertThat(queryResult.data.int64Variants)
      .isEqualTo(
        GetInt64variantsByIdQuery.Data.Int64variants(
          nonNullWithZeroValue = 0,
          nonNullWithPositiveValue = 4242424242424242,
          nonNullWithNegativeValue = -4242424242424242,
          nonNullWithMaxValue = Long.MAX_VALUE,
          nonNullWithMinValue = Long.MIN_VALUE,
          nullableWithNullValue = null,
          nullableWithZeroValue = 0,
          nullableWithPositiveValue = 2424242424242424,
          nullableWithNegativeValue = -2424242424242424,
          nullableWithMaxValue = Long.MAX_VALUE,
          nullableWithMinValue = Long.MIN_VALUE,
          emptyList = emptyList(),
          nonEmptyList = listOf(0, -1, 1, 99, -99, Long.MIN_VALUE, Long.MAX_VALUE)
        )
      )
  }

  @Test
  fun uuidVariants() = runTest {
    val id = randomAlphanumericString()
    val nonNullValue = UUID.randomUUID()
    val nullableWithNullValue = UUID.randomUUID()
    val nonEmptyList = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())

    connector.insertUuidvariants.execute(
      id = id,
      nonNullValue = nonNullValue,
      nullableWithNullValue = nullableWithNullValue,
      nullableWithNonNullValue = null,
      emptyList = emptyList(),
      nonEmptyList = nonEmptyList
    )

    val queryResult = connector.getUuidvariantsById.execute(id)
    assertThat(queryResult.data.uUIDVariants)
      .isEqualTo(
        GetUuidvariantsByIdQuery.Data.Uuidvariants(
          nonNullValue = nonNullValue,
          nullableWithNullValue = nullableWithNullValue,
          nullableWithNonNullValue = null,
          emptyList = emptyList(),
          nonEmptyList = nonEmptyList
        )
      )
  }

  @Test
  fun dateVariantsInVariables() = runTest {
    val id = randomAlphanumericString()

    connector.insertDateVariants.execute(
      id = id,
      nonNullValue = dateFromString("2024-04-26"),
      nullableWithNullValue = null,
      nullableWithNonNullValue = dateFromString("2024-05-19"),
      minValue = dateFromString("0001-01-01"),
      maxValue = dateFromString("9999-12-31"),
      emptyList = emptyList(),
      nonEmptyList = listOf("1234-05-19", "5678-12-31").map(::dateFromString)
    )

    val queryRef = connector.getDateVariantsById.ref(id).withStringData()
    val queryResult = queryRef.execute()
    assertThat(queryResult.data.dateVariants)
      .isEqualTo(
        GetDateVariantsByIdQueryStringData.DateVariants(
          nonNullValue = "2024-04-26",
          nullableWithNullValue = null,
          nullableWithNonNullValue = "2024-05-19",
          minValue = "0001-01-01",
          maxValue = "9999-12-31",
          emptyList = emptyList(),
          nonEmptyList = listOf("1234-05-19", "5678-12-31")
        )
      )
  }

  @Test
  fun timestampVariants() = runTest {
    val id = randomAlphanumericString()

    connector.insertTimestampVariants.execute(
      id = id,
      nonNullValue = Timestamp(1, 3_219),
      nullableWithNullValue = null,
      nullableWithNonNullValue = Timestamp(-46_239, 4_628),
      minValue = Timestamp(-62_135_596_800, 0),
      maxValue = Timestamp(253_402_300_799, 999_999_999),
      // TODO: Change to emptyList() when emulator bug fixes
      emptyList = listOf(Timestamp(-543, 41)),
      nonEmptyList = listOf(Timestamp(-543, 41), Timestamp(739, 62))
    )

    val queryResult = connector.getTimestampVariantsById.execute(id)

    /**
     * Note: Timestamp sent from SDK is always 9 digits nanosecond precision, meaning there are 9
     * digits in SSSSSSSSS parts. However, when running against different databases, this precision
     * might change, and server will truncate it to 0/3/6 digits precision without throwing an
     * error. That's why in the integration test, we only verify the second. Serializer will be
     * tested in unit tests.
     */
    assertTrue(
      queryResult.data.timestampVariants!!.verifySeconds(
        GetTimestampVariantsByIdQuery.Data.TimestampVariants(
          nonNullValue = Timestamp(1, 3_219),
          nullableWithNullValue = null,
          nullableWithNonNullValue = Timestamp(-46_239, 4_628),
          minValue = Timestamp(-62_135_596_800, 0),
          maxValue = Timestamp(253_402_300_799, 999_999_999),
          // TODO: Change to emptyList() when emulator bug fixes
          emptyList = listOf(Timestamp(-543, 41)),
          nonEmptyList = listOf(Timestamp(-543, 41), Timestamp(739, 62))
        )
      )
    )
  }

  @Test
  fun dateVariantsInData() = runTest {
    val id = randomAlphanumericString()

    val mutationRef =
      connector.insertDateVariants.refWith(
        InsertDateVariantsMutationStringVariables(
          id = id,
          nonNullValue = "2024-04-26",
          nullableWithNullValue = null,
          nullableWithNonNullValue = "2024-05-19",
          minValue = "0001-01-01",
          maxValue = "9999-12-31",
          emptyList = emptyList(),
          nonEmptyList = listOf("1234-05-19", "5678-12-31")
        )
      )
    mutationRef.execute()

    val queryResult = connector.getDateVariantsById.execute(id)
    assertThat(queryResult.data.dateVariants)
      .isEqualTo(
        GetDateVariantsByIdQuery.Data.DateVariants(
          nonNullValue = dateFromString("2024-04-26"),
          nullableWithNullValue = null,
          nullableWithNonNullValue = dateFromString("2024-05-19"),
          minValue = dateFromString("0001-01-01"),
          maxValue = dateFromString("9999-12-31"),
          emptyList = emptyList(),
          nonEmptyList = listOf("1234-05-19", "5678-12-31").map(::dateFromString)
        )
      )
  }

  // TODO: Repeat the tests above for Int, Float, and Boolean.
  //  Make sure to test boundary values, like Int.MAX_VALUE, Float.NaN, true, and false.

  private companion object {
    fun dateFromString(s: String): Date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s)!!

    fun QueryRef<*, GetDateVariantsByIdQuery.Variables>.withStringData() =
      dataConnect.query(
        operationName,
        variables,
        serializer<GetDateVariantsByIdQueryStringData>(),
        variablesSerializer
      )

    fun InsertDateVariantsMutation.refWith(variables: InsertDateVariantsMutationStringVariables) =
      connector.dataConnect.mutation(
        InsertDateVariantsMutation.operationName,
        variables,
        InsertDateVariantsMutation.dataDeserializer,
        serializer()
      )

    fun GetTimestampVariantsByIdQuery.Data.TimestampVariants.verifySeconds(
      other: GetTimestampVariantsByIdQuery.Data.TimestampVariants
    ): Boolean {
      val properties = GetTimestampVariantsByIdQuery.Data.TimestampVariants::class.memberProperties
      for (field in properties) {
        val actual = field.get(this)
        val expect = field.get(other)
        if (actual == null && expect == null) {
          continue
        } else if (actual is Timestamp && expect is Timestamp) {
          if (!comparedSerializedTimestamp(actual, expect)) {
            return false
          }
        } else if (actual is List<*> && expect is List<*>) {
          if (actual.size != expect.size) {
            return false
          }
          for (i in 0 until actual.size) {
            if (!comparedSerializedTimestamp(actual[i] as Timestamp, expect[i] as Timestamp)) {
              return false
            }
          }
        } else {
          throw IllegalArgumentException(
            "`TimestampVariants.verifySeconds` doesn't support these types."
          )
        }
      }
      return true
    }

    private fun comparedSerializedTimestamp(actual: Timestamp, expect: Timestamp): Boolean {
      return actual.seconds == expect.seconds
    }
  }

  /**
   * An alternative to [InsertDateVariantsMutation.Variables] where the fields are typed as [String]
   * instead of [Date].
   */
  @Serializable
  data class InsertDateVariantsMutationStringVariables(
    val id: String,
    val nonNullValue: String,
    val nullableWithNullValue: String?,
    val nullableWithNonNullValue: String?,
    val minValue: String,
    val maxValue: String,
    val emptyList: List<String>,
    val nonEmptyList: List<String>
  )

  /**
   * An alternative to [GetDateVariantsByIdQuery.Data] where the fields are typed as [String]
   * instead of [Date].
   */
  @Serializable
  private data class GetDateVariantsByIdQueryStringData(val dateVariants: DateVariants?) {

    @Serializable
    data class DateVariants(
      val nonNullValue: String,
      val nullableWithNullValue: String?,
      val nullableWithNonNullValue: String?,
      val minValue: String,
      val maxValue: String,
      val emptyList: List<String>,
      val nonEmptyList: List<String>
    )
  }
}