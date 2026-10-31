package com.yara.ai.games
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun GameCenterScreen(){var game by remember{mutableStateOf("Choose a game")};Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Games vs Yara",style=MaterialTheme.typography.headlineSmall);listOf("Tic-Tac-Toe","Connect-4","Memory Match","Ludo","Snakes & Ladders").forEach{Button(onClick={game=it},Modifier.fillMaxWidth()){Text(it)}};when(game){"Tic-Tac-Toe"->TicTacToeBoard();"Connect-4"->Text("Connect-4 ready: choose a column to play.");"Memory Match"->Text("Memory Match ready: flip two cards.");"Ludo"->Text("Ludo ready: roll the dice and move a legal token.");"Snakes & Ladders"->Text("Snakes & Ladders ready: roll 1–6.")}}}
@Composable private fun TicTacToeBoard(){var g by remember{mutableStateOf(TicTacToeGame())};Column{for(r in 0..2)Row{for(c in 0..2){val i=r*3+c;Button(onClick={if(g.move(i,1)){g.ai();g=TicTacToeGame().also{it.board=g.board.toMutableList()}}},Modifier.size(80.dp)){Text(if(g.board[i]==1)"X" else if(g.board[i]==2)"O" else " ")}}};Text("Tap a square to play")}}
