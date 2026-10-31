package com.yara.ai.games
import kotlin.random.Random
data class LudoToken(var pos:Int=-1)
class LudoGame{val tokens=Array(2){Array(4){LudoToken()}};var turn=0;var lastDie=0;fun roll():Int{lastDie=Random.nextInt(1,7);return lastDie};fun move(token:Int):Boolean{if(token !in 0..3||lastDie==0)return false;val t=tokens[turn][token];if(t.pos<0&&lastDie!=6)return false;t.pos=if(t.pos<0)0 else t.pos+lastDie;if(t.pos>56)t.pos=56;turn=1-turn;lastDie=0;return true}}
