package com.google.firebase.dataconnect.testutil

import com.google.firebase.dataconnect.QueryRef
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer

fun <Data, Variables, NewData> QueryRef<Data, Variables>.withDataDeserializer(
  newDataDeserializer: DeserializationStrategy<NewData>
): QueryRef<NewData, Variables> =
  dataConnect.query(
    operationName = operationName,
    variables = variables,
    dataDeserializer = newDataDeserializer,
    variablesSerializer = variablesSerializer
  )

fun <Data, NewVariables> QueryRef<Data, *>.withVariables(
  variables: NewVariables,
  serializer: SerializationStrategy<NewVariables>
): QueryRef<Data, NewVariables> =
  dataConnect.query(
    operationName = operationName,
    variables = variables,
    dataDeserializer = dataDeserializer,
    variablesSerializer = serializer
  )

inline fun <Data, reified NewVariables> QueryRef<Data, *>.withVariables(
  variables: NewVariables
): QueryRef<Data, NewVariables> = withVariables(variables, serializer())
