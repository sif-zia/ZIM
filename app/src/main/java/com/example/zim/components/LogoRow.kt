package com.example.zim.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zim.R

@Composable
fun LogoRow(
    modifier: Modifier = Modifier,
    expandMenu: (() -> Unit)? = null,
    dropDown: (@Composable () -> Unit )? = null
) {
    return Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .height(44.dp)
                .width(44.dp),
            painter = painterResource(id = R.drawable.zim_logo),
            contentDescription = "ZIM Logo"
        )
        Text(
            text = "ZIM",
            fontSize = 22.sp
        )

        if (dropDown != null)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .width(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column {
                    Image(
                        modifier = Modifier.clickable {
                            if (expandMenu != null)
                                expandMenu()
                        },
                        painter = painterResource(id = R.drawable.menu_icon),
                        contentDescription = "Menu Icon"
                    )
                    dropDown()
                }
            }
    }
}