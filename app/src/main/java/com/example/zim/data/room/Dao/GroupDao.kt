package com.example.zim.data.room.Dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.zim.data.room.models.GroupMemberships
import com.example.zim.data.room.models.Groups
import com.example.zim.helperclasses.Group
import com.example.zim.helperclasses.GroupWithPendingInvite
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Insert
    suspend fun insertGroup(group: Groups): Long

    @Insert
    suspend fun insertGroupMember(groupMemberships: GroupMemberships): Long

    @Query("SELECT * FROM Groups WHERE Group_ID = :groupId")
    suspend fun getGroupById(groupId: Int): Groups

    @Transaction
    @Query(
        """
    SELECT 
        group_info.Group_ID AS groupId,
        group_info.name AS groupName,
        MAX(group_info.latest_message_time) AS lastMessageTime,
        group_info.latest_message_content AS lastMessage,
        group_info.cover AS coverUri,
        SUM(group_info.unread_msgs) AS unreadMessages,
        group_info.latest_message_type AS lastMessageType
    FROM (
            SELECT 
                g.Group_ID,
                g.name,
                g.cover,
                MAX(sm.sentTime) AS latest_message_time,
                (SELECT msg FROM Messages WHERE Message_ID = sm.Message_ID_FK) AS latest_message_content,
                (SELECT type FROM Messages WHERE Message_ID = sm.Message_ID_FK) AS latest_message_type,
                0 AS unread_msgs
            FROM Groups g
            JOIN Group_Memberships gm ON gm.Group_ID_FK = g.Group_ID
            JOIN Curr_User cu ON gm.User_ID_FK = cu.User_ID_FK
            LEFT JOIN Sent_Messages sm ON sm.Message_ID_FK IN (
                SELECT gmr.Message_ID_FK 
                FROM Group_Message_Receivers gmr 
                WHERE gmr.Group_ID_FK = g.Group_ID
            )
            WHERE g.name LIKE "%" || :query || "%"
            GROUP BY g.Group_ID, g.name, g.cover

            UNION ALL

            SELECT 
                g.Group_ID,
                g.name,
                g.cover,
                MAX(rm.receivedTime) AS latest_message_time,
                (SELECT msg FROM Messages WHERE Message_ID = rm.Message_ID_FK) AS latest_message_content,
                (SELECT type FROM Messages WHERE Message_ID = rm.Message_ID_FK) AS latest_message_type,
                SUM(CASE WHEN rm.isRead = 0 THEN 1 ELSE 0 END) AS unread_msgs
            FROM Groups g
            JOIN Group_Memberships gm ON gm.Group_ID_FK = g.Group_ID
            JOIN Curr_User cu ON gm.User_ID_FK = cu.User_ID_FK
            LEFT JOIN Received_Messages rm ON rm.Message_ID_FK IN (
                SELECT gmr.Message_ID_FK 
                FROM Group_Message_Receivers gmr 
                WHERE gmr.Group_ID_FK = g.Group_ID
            )
            WHERE g.name LIKE "%" || :query || "%"
            GROUP BY g.Group_ID, g.name, g.cover
        ) AS group_info
    GROUP BY group_info.Group_ID, group_info.name
    ORDER BY latest_message_time DESC
"""
    )
    fun getGroupsWithLatestMessage(query: String): Flow<List<Group>>

    @Query(
        """
        UPDATE Group_Memberships
        SET hasReceivedInvitation = 1
        WHERE Group_ID_FK = (SELECT Group_ID FROM Groups WHERE secretKey = :groupSecretKey)
        AND User_ID_FK = (SELECT User_ID FROM Users WHERE UUID = :userPuKey)"""
    )
    suspend fun setGroupInviteReceived(groupSecretKey: String, userPuKey: String)


    @Transaction
    @Query(
        """
        SELECT 
            g.Group_ID AS groupId,
            g.name AS groupName,
            g.description AS groupDescription,
            g.secretKey AS groupSecretKey,
            u.User_ID AS userId,
            u.fName AS firstName,
            u.lName AS lastName,
            u.UUID AS puKey,
            (SELECT admin_user.UUID FROM Users admin_user WHERE admin_user.User_ID = g.User_ID_FK) AS adminPuKey
        FROM Groups g
        JOIN Group_Memberships gm ON g.Group_ID = gm.Group_ID_FK
        JOIN Users u ON gm.User_ID_FK = u.User_ID
        WHERE g.Group_ID IN (
            SELECT gm.Group_ID_FK
            FROM Group_Memberships gm
            WHERE gm.hasReceivedInvitation = 0
        )
        ORDER BY g.Group_ID
"""
    )
    fun getGroupsWithPendingInvitations(): Flow<List<GroupWithPendingInvite>>

    @Query(
        """
    SELECT 
        u.UUID AS publicKey
    FROM Users u
    JOIN Group_Memberships gm ON u.User_ID = gm.User_ID_FK
    WHERE gm.hasReceivedInvitation = 0
    GROUP BY u.UUID
"""
    )
    fun getUsersWithPendingGroupInvitations(): Flow<List<String>>

    @Query("SELECT EXISTS (SELECT 1 FROM Groups WHERE secretKey = :groupSecretKey)")
    suspend fun checkIfGroupExists(groupSecretKey: String): Boolean

    /**
     * Get all user IDs for members of a specific group
     */
    @Query("""
        SELECT User_ID_FK
        FROM Group_Memberships
        WHERE Group_ID_FK = :groupId
    """)
    suspend fun getGroupMemberIds(groupId: Int): List<Int>

    @Query("SELECT Group_ID FROM Groups WHERE secretKey = :secretKey LIMIT 1")
    suspend fun getGroupIdBySecretKey(secretKey: String): Int?
}