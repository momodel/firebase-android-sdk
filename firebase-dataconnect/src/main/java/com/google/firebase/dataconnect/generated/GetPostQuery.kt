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
package com.google.firebase.dataconnect.generated

import com.google.firebase.dataconnect.FirebaseDataConnect
import com.google.firebase.dataconnect.QueryRef
import com.google.firebase.dataconnect.QuerySubscription
import com.google.firebase.dataconnect.ResultDecodeException

class GetPostQuery(dataConnect: FirebaseDataConnect) :
  QueryRef<GetPostQuery.Variables, GetPostQuery.Result>(
    dataConnect,
    operationName = "getPost",
    operationSet = "crud",
    revision = "1234567890abcdef"
  ) {

  data class Variables(val id: String) {
    val builder = Builder(id = id)
    fun build(block: Builder.() -> Unit): Variables = builder.apply(block).build()
    class Builder(var id: String) {
      fun build() = Variables(id = id)
    }
  }

  data class Result(val post: Post) {
    data class Post(val content: String, val comments: List<Comment>) {
      data class Comment(val id: String, val content: String)
    }
  }

  override fun encodeVariables(variables: Variables) = mapOf("id" to variables.id)

  override fun decodeResult(map: Map<String, Any?>) =
    Result(
      map["post"].let {
        _decodePost(
          data = it as? Map<*, *> ?: decodeError("post", it, "Map", "result", map),
          path = "post"
        )
      }
    )

  private fun _decodePost(data: Map<*, *>, path: String) =
    Result.Post(
      content =
        data["content"].let {
          it as? String ?: decodeError("$path.content", it, "String", path, data)
        },
      comments =
        data["comments"].let {
          _decodeComments(
            it as? List<*> ?: decodeError("$path.comments", it, "List", path, data),
            "$path.comments"
          )
        }
    )

  private fun _decodeComments(data: List<*>, path: String) =
    data.mapIndexed { index, it ->
      _decodeComment(
        it as? Map<*, *> ?: decodeError("$path[$index]", it, "Map", path, data),
        "$path[$index]"
      )
    }

  private fun _decodeComment(data: Map<*, *>, path: String) =
    Result.Post.Comment(
      id = data["id"].let { it as? String ?: decodeError("$path.id", it, "String", path, data) },
      content =
        data["content"].let {
          it as? String ?: decodeError("$path.content", it, "String", path, data)
        }
    )

  private fun decodeError(
    path: String,
    actual: Any?,
    expected: String,
    contextName: String,
    context: Any?
  ): Nothing =
    throw ResultDecodeException(
      "parsing GetPostQuery.Result failed: \"$path\" was expected to be $expected, " +
        "but got $actual ($contextName=$context)"
    )
}

typealias GetPostQuerySubscription = QuerySubscription<GetPostQuery.Variables, GetPostQuery.Result>

val FirebaseDataConnect.Queries.getPost: GetPostQuery
  get() = GetPostQuery(dataConnect)

suspend fun GetPostQuery.execute(id: String): GetPostQuery.Result = execute(variablesFor(id = id))

fun GetPostQuery.subscribe(
  id: String
): QuerySubscription<GetPostQuery.Variables, GetPostQuery.Result> = subscribe(variablesFor(id = id))

fun GetPostQuerySubscription.update(block: GetPostQuery.Variables.Builder.() -> Unit) =
  update(variables.build(block))

private fun variablesFor(id: String) = GetPostQuery.Variables(id = id)
