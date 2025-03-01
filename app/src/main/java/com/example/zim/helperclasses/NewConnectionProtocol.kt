package com.example.zim.helperclasses

import com.example.zim.data.room.models.Users
import com.example.zim.utils.Package

class NewConnectionProtocol(
    var currentUser: Users,
    val onProtocolStart: () -> Unit,
    val onProtocolEnd: (newUser: Users) -> Unit,
    val onProtocolError: (error: String) -> Unit,
    val onExistingConnection: (uuid: String) -> Unit,
    val sendProtocolMessage: (stepNo: Int,  message: String) -> Unit,
    var connectedUser: Users? = null,
    var isGroupOwner: Boolean? = null,
    var uuids: List<String> = emptyList(),
    var stepNo: Int = 0
) {

    fun initUser(isGroupOwner: Boolean) {
        this.connectedUser = Users(
            fName = "",
            lName = "",
            UUID = "",
            deviceName = "",
            deviceAddress = ""
        )
        this.isGroupOwner = isGroupOwner
        this.stepNo = 0
    }

    fun startProtocol(uuids: List<String>) {
        this.uuids = uuids

        if (isGroupOwner == true) {
            serverStep()
        }

        onProtocolStart()
    }

    fun processStep(receiveData: Package.Type.Protocol) {
        val step = receiveData.stepNumber
        val data = receiveData.msg

        if (step != stepNo) {
            onProtocolError("Step number mismatch expected ${stepNo}, received $step")
            return
        }

        if (isGroupOwner == true) {
            serverStep(data)
        } else {
            clientStep(data)
        }
    }

    fun serverStep(receiveData: String? = null) {
        when (stepNo) {
            0 -> { // Send Hello
                sendProtocolMessage(0,"Hello")
                stepNo++
            }

            1 -> { // Receive Hello Back and Send UUID
                if (receiveData == "Hello Back") {
                    sendProtocolMessage(1,"${currentUser.UUID}")
                    stepNo++
                } else {
                    onProtocolError("Expected Hello Back, received $receiveData")
                }
            }

            2 -> { // Receive UUID and send deviceName
                if (receiveData != null && receiveData.length == 36) {
                    if(receiveData in uuids) {
                        onExistingConnection(receiveData)
                        return
                    }

                    connectedUser = connectedUser?.copy(UUID = receiveData)
                    sendProtocolMessage(2,":${currentUser.deviceName}")
                    stepNo++
                } else {
                    onProtocolError("Expected UUID, received $receiveData")
                }
            }

            3 -> { // Receive deviceName and send fName
                if (!receiveData.isNullOrEmpty()  && receiveData != "null") {
                    connectedUser = connectedUser?.copy(deviceName = receiveData)
                    sendProtocolMessage(3,"${currentUser.fName}")
                    stepNo++
                } else {
                    onProtocolError("Expected deviceName, received $receiveData")
                }
            }

            4 -> { // Receive fName and Send lName
                if (!receiveData.isNullOrEmpty() && receiveData != "null") {
                    connectedUser = connectedUser?.copy(fName = receiveData)
                    sendProtocolMessage(4,"${currentUser.lName}")
                    stepNo++
                } else {
                    onProtocolError("Expected fName, received $receiveData")
                }
            }

            5 -> { // Receive lName
                if (!receiveData.isNullOrEmpty() && receiveData != "null") {
                    connectedUser = connectedUser?.copy(lName = receiveData)
                    stepNo++
                    if(connectedUser != null) {
                        onProtocolEnd(connectedUser!!)
                    } else {
                        onProtocolError("Connected user is null")
                    }
                } else {
                    onProtocolError("Expected lName, received $receiveData")
                }
            }
        }
    }

    fun clientStep(receiveData: String? = null) {
        when (stepNo) {
            0 -> { // Receive Hello and send Hello Back
                if (receiveData == "Hello") {
                    sendProtocolMessage(1,"Hello Back")
                    stepNo++
                } else {
                    onProtocolError("Expected Hello, received $receiveData")
                }
            }

            1 -> { // Receive UUID and send UUID
                if (receiveData != null && receiveData.length == 36) {
                    if(receiveData in uuids) {
                        sendProtocolMessage(2,"${currentUser.UUID}")
                        onExistingConnection(receiveData)
                        return
                    }
                    connectedUser = connectedUser?.copy(UUID = receiveData)
                    sendProtocolMessage(2,"${currentUser.UUID}")
                    stepNo++
                } else {
                    onProtocolError("Expected UUID, received $receiveData")
                }
            }

            2 -> { // Receive deviceName and send deviceName
                if (!receiveData.isNullOrEmpty() && receiveData != "null") {
                    connectedUser = connectedUser?.copy(deviceName = receiveData)
                    sendProtocolMessage(3,"${currentUser.deviceName}")
                    stepNo++
                } else {
                    onProtocolError("Expected deviceName, received $receiveData")
                }
            }

            3 -> { // Receive fName and send fName
                if (!receiveData.isNullOrEmpty() && receiveData != "null") {
                    connectedUser = connectedUser?.copy(fName = receiveData)
                    sendProtocolMessage(4,"${currentUser.fName}")
                    stepNo++
                } else {
                    onProtocolError("Expected fName, received $receiveData")
                }
            }

            4 -> { // Receive lName and send lName
                if (!receiveData.isNullOrEmpty() && receiveData != "null") {
                    connectedUser = connectedUser?.copy(lName = receiveData)
                    sendProtocolMessage(5,"${currentUser.lName}")
                    stepNo++
                    if(connectedUser != null) {
                        onProtocolEnd(connectedUser!!)
                    } else {
                        onProtocolError("Connected user is null")
                    }
                } else {
                    onProtocolError("Expected lName, received $receiveData")
                }
            }
        }
    }
}