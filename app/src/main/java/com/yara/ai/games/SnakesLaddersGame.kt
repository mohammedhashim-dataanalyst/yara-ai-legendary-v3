package com.yara.ai.games
import kotlin.random.Random
class SnakesLaddersGame{var player=1;var yara=1;var turn=0;val jumps=mapOf(4 to 14,9 to 31,17 to 7,20 to 38,28 to 84,40 to 59,51 to 67,54 to 34,62 to 19,64 to 60,63 to 81,87 to 24,93 to 73,95 to 75,99 to 78);fun roll():Int{val d=Random.nextInt(1,7);if(turn==0)player=advance(player,d)else yara=advance(yara,d);turn=1-turn;return d};private fun advance(p:Int,d:Int):Int{val n=p+d;if(n>100)return p;return jumps[n]?:n};fun winner()=when{player==100->1;yara==100->2;else->0}}
