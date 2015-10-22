/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql

import scala.language.postfixOps

import org.apache.spark.sql.functions._
import org.apache.spark.sql.test.SharedSQLContext

case class ClassData(a: String, b: Int)

class DatasetSuite extends QueryTest with SharedSQLContext {
  import testImplicits._

  test("toDS") {
    val data = Seq(("a", 1) , ("b", 2), ("c", 3))
    checkAnswer(
      data.toDS(),
      data: _*)
  }

  test("as case class / collect") {
    val ds = Seq(("a", 1) , ("b", 2), ("c", 3)).toDF("a", "b").as[ClassData]
    checkAnswer(
      ds,
      ClassData("a", 1), ClassData("b", 2), ClassData("c", 3))
    assert(ds.collect().head == ClassData("a", 1))
  }

  test("as case class - reordered fields by name") {
    val ds = Seq((1, "a"), (2, "b"), (3, "c")).toDF("b", "a").as[ClassData]
    assert(ds.collect() === Array(ClassData("a", 1), ClassData("b", 2), ClassData("c", 3)))
  }

  test("map") {
    val ds = Seq(("a", 1) , ("b", 2), ("c", 3)).toDS()
    checkAnswer(
      ds.map(v => (v._1, v._2 + 1)),
      ("a", 2), ("b", 3), ("c", 4))
  }

  test("select") {
    val ds = Seq(("a", 1) , ("b", 2), ("c", 3)).toDS()
    checkAnswer(
      ds.select(expr("_2 + 1").as[Int]),
      2, 3, 4)
  }

  test("select 3") {
    val ds = Seq(("a", 1) , ("b", 2), ("c", 3)).toDS()
    checkAnswer(
      ds.select(
        expr("_1").as[String],
        expr("_2").as[Int],
        expr("_2 + 1").as[Int]),
      ("a", 1, 2), ("b", 2, 3), ("c", 3, 4))
  }

  test("filter") {
    val ds = Seq(("a", 1) , ("b", 2), ("c", 3)).toDS()
    checkAnswer(
      ds.filter(_._1 == "b"),
      ("b", 2))
  }

  test("foreach") {
    val ds = Seq(("a", 1) , ("b", 2), ("c", 3)).toDS()
    val acc = sparkContext.accumulator(0)
    ds.foreach(v => acc += v._2)
    assert(acc.value == 6)
  }

  test("foreachPartition") {
    val ds = Seq(("a", 1) , ("b", 2), ("c", 3)).toDS()
    val acc = sparkContext.accumulator(0)
    ds.foreachPartition(_.foreach(v => acc += v._2))
    assert(acc.value == 6)
  }

  test("reduce") {
    val ds = Seq(("a", 1) , ("b", 2), ("c", 3)).toDS()
    assert(ds.reduce((a, b) => ("sum", a._2 + b._2)) == ("sum", 6))
  }

  test("fold") {
    val ds = Seq(("a", 1) , ("b", 2), ("c", 3)).toDS()
    assert(ds.fold(("", 0))((a, b) => ("sum", a._2 + b._2)) == ("sum", 6))
  }

  test("groupBy function, keys") {
    val ds = Seq(("a", 1), ("b", 1)).toDS()
    val grouped = ds.groupBy(v => (1, v._2))
    checkAnswer(
      grouped.keys,
      (1, 1))
  }

  test("groupBy function, mapGroups") {
    val ds = Seq(("a", 10), ("a", 20), ("b", 1), ("b", 2), ("c", 1)).toDS()
    val grouped = ds.groupBy(v => (v._1, "word"))
    val agged = grouped.mapGroups { case (g, iter) =>
      Iterator((g._1, iter.map(_._2).sum))
    }

    checkAnswer(
      agged,
      ("a", 30), ("b", 3), ("c", 1))
  }
}
