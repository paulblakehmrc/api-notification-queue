/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apinotificationqueue.repository

import play.api.Logger
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.apinotificationqueue.model.Notification

trait NotificationRepositoryErrorHandler {

  def handleDeleteError(result: WriteResult, exceptionMsg: => String): Boolean = {
    handleError(result, databaseAltered, exceptionMsg)
  }

  def handleSaveError(writeResult: WriteResult, exceptionMsg: => String, notification: => Notification): Notification = {

    def handleSaveError(result: WriteResult): Notification =
      if (databaseAltered(result))
        notification
      else
        throw new RuntimeException(exceptionMsg)

    handleError(writeResult, handleSaveError, exceptionMsg)
  }

  private def handleError[T](result: WriteResult, f: WriteResult => T, exceptionMsg: => String): T = {
    result.writeConcernError.fold(f(result)) {
      errMsg => {
        val errorMsg = s"$exceptionMsg. $errMsg"
        Logger.error(errorMsg)
        throw new RuntimeException(errorMsg)
      }
    }
  }

  private def databaseAltered(writeResult: WriteResult): Boolean = writeResult.n > 0

}
