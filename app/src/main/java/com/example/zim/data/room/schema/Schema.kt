package com.example.zim.data.room.schema

class Schema {
    companion object {
        const val DB_NAME: String = "ZIM_Database"
        const val USERS_TABLE: String = "Users"
        const val USER_ID: String = "User_ID"
        const val USER_ID_FK: String = "User_ID_FK"
        const val CURR_USER: String = "Curr_User"
        const val CURR_USER_ID: String = "Curr_User_ID"
        const val MESSAGES: String = "Messages"
        const val MESSAGE_ID: String = "Message_ID"
        const val MESSAGE_ID_FK: String = "Message_ID_FK"
        const val SENT_MESSAGES: String = "Sent_Messages"
        const val SENT_MESSAGE_ID: String = "Sent_Messages_ID"
        const val RECEIVED_MESSAGES: String = "Received_Messages"
        const val RECEIVED_MESSAGE_ID: String = "Received_Messages_ID"

    }
}
