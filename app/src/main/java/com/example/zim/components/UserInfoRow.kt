package com.example.zim.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.compose.rememberAsyncImagePainter
import com.example.zim.R
import com.example.zim.navigation.DropDownMenus
import com.example.zim.navigation.Navigation

fun navigateTo(navController: NavController, navigation: Navigation) {
    navController.navigate(navigation.route) {
        popUpTo(navController.graph.findStartDestination().id)
        launchSingleTop = true
        restoreState = true
    }
}

fun Color.add(offset: Float): Color {
    val normalizedOffset = offset / 255f // Convert the offset to the normalized range
    return this.copy(
        red = (this.red + normalizedOffset).coerceIn(0f, 1f),
        green = (this.green + normalizedOffset).coerceIn(0f, 1f),
        blue = (this.blue + normalizedOffset).coerceIn(0f, 1f)
    )
}

@Composable
fun UserInfoRow(username: String, status: Int, userDp: Uri?, navController: NavController) {
    var isExpanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 9.dp,
                shape = RoundedCornerShape(0.dp),
                clip = false
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondary.add(24f))
                .padding(8.dp)
                .height(50.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Icon
            IconButton(onClick = { navigateTo(navController, Navigation.Chats) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = "Back Button",
                    modifier = Modifier
                        .size(28.dp)
                )
            }
            // Display Picture
            Image(
                painter = rememberAsyncImagePainter(
                    model = userDp,
                    placeholder = painterResource(R.drawable.dp_icon),
                    error = painterResource(id = R.drawable.dp_icon)
                ),
                contentDescription = "Display Picture",
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(100))
                    .size(42.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Username
                    Text(text = username, fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)

                    // Status
                    if (status == 1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(100))
                                    .background(Color.Green)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Connected", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.66f))
                        }
                    else if (status == 2)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(100))
                                    .background(Color.Yellow)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "In Network", fontSize = 16.sp)
                        }
                }

                Column {
                    IconButton(onClick = {
                        isExpanded = true
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = "Menu Icon",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    DropDown(
                        dropDownMenu = DropDownMenus.UserChatScreen(),
                        navController = navController,
                        expanded = isExpanded,
                        dismissMenu = {isExpanded = false}
                    )
                }
            }
        }
    }
}