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
        const val ALERTS: String = "Alerts"
        const val ALERTS_ID : String = "Alerts_ID"
        const val ALERTS_ID_FK :String = "Alerts_ID_FK"
        const val RECEIVED_ALERTS: String = "Received_Alerts"
        const val RECEIVED_ALERTS_ID: String = "Received_Alerts_ID"
        const val GROUPS: String = "Groups"
        const val GROUP_ID: String = "Group_ID"
        const val GROUP_ID_FK: String = "Group_ID_FK"
        const val GROUP_MEMBERSHIPS: String = "Group_Memberships"
        const val GROUP_MEMBERSHIPS_ID: String = "Group_Memberships_ID"
        const val GROUP_MESSAGE_RECEIVERS: String = "Group_Message_Receivers"
        const val GROUP_MESSAGE_RECEIVERS_ID: String = "Group_Message_Receivers_ID"
    }
}
