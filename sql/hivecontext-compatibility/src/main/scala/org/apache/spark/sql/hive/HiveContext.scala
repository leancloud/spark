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

package org.apache.spark.sql.hive

import org.apache.spark.SparkContext
import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.internal.Logging
import org.apache.spark.sql.{SparkSession, SQLContext}


/**
 * An instance of the Spark SQL execution engine that integrates with data stored in Hive.
 * Configuration for Hive is read from hive-site.xml on the classpath.
 */
@deprecated("Use SparkSession.withHiveSupport instead", "2.0.0")
class HiveContext private[hive](
    @transient private val sparkSession: SparkSession,
    isRootContext: Boolean)
  extends SQLContext(sparkSession, isRootContext) with Logging {

  self =>

  def this(sc: SparkContext) = {
    this(new SparkSession(HiveUtils.withHiveExternalCatalog(sc)), true)
  }

  def this(sc: JavaSparkContext) = this(sc.sc)

  /**
   * Returns a new HiveContext as new session, which will have separated SQLConf, UDF/UDAF,
   * temporary tables and SessionState, but sharing the same CacheManager, IsolatedClientLoader
   * and Hive client (both of execution and metadata) with existing HiveContext.
   */
  override def newSession(): HiveContext = {
    new HiveContext(sparkSession.newSession(), isRootContext = false)
  }

  protected[sql] override def sessionState: HiveSessionState = {
    sparkSession.sessionState.asInstanceOf[HiveSessionState]
  }

  protected[sql] override def sharedState: HiveSharedState = {
    sparkSession.sharedState.asInstanceOf[HiveSharedState]
  }

}
