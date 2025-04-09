package com.example.purrytify.ui.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.purrytify.R
import com.example.purrytify.data.TokenManager
import com.example.purrytify.ui.model.LoginViewModel
import com.example.purrytify.navigation.Screen
import com.example.purrytify.ui.theme.Poppins

@Composable
fun LoginScreen(navController: NavHostController, viewModel: LoginViewModel = viewModel(factory = LoginViewModel.provideFactory())) {
    val context = LocalContext.current
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.errorMessage = null
        }
    }

    LaunchedEffect(Unit) {
        val exists = viewModel.checkIsTokenExist()
        if (exists) {
            viewModel.validateToken(
                onValid = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onRefreshFailed = {
                    // Stay on login
                }
            )
        }
    }

    if (viewModel.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter // this centers children
            ) {
                Image(
                    painter = painterResource(id = R.drawable.login_bg),
                    contentDescription = "Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Image(
                    painter = painterResource(id = R.drawable.logo_3),
                    contentDescription = "Logo",
                    modifier = Modifier.size(92.dp).background(Color(0xFF121212))
                )
            }

            Text(
                "Millions of Song.",
                style = TextStyle(
                    fontFamily = Poppins,
                    fontWeight = FontWeight(700),
                    fontSize = 24.sp,
                )
            )
            Text(
                "Only on Purrytify.",
                style = TextStyle(
                    fontFamily = Poppins,
                    fontWeight = FontWeight(700),
                    fontSize = 24.sp,
                )
            )
            Column (verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f).navigationBarsPadding().padding(horizontal = 16.dp)) {
                Text(
                    "Email",
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    ),
                )
                OutlinedTextField(
                    value = viewModel.email,
                    onValueChange = { viewModel.email = it },
                    placeholder = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.DarkGray,
                        cursorColor = Color.White,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )
                Spacer(Modifier.height(14.dp))

                Text(
                    "Password",
                    style = TextStyle(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    ),
                )
                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.password = it },
                    placeholder = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.DarkGray,
                        cursorColor = Color.White,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray,
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        viewModel.login {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1ED760),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .height(52.dp)
                ) {
                    Text("Log In", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = Poppins)
                }
            }
        }
    }
}
