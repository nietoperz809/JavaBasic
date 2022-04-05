10 REM written and tested in SmallBasic
20 REM modified for Tinycat 2017-01-22 [B+=MGA]
30 REM ===================== title and instructions
40 PRINT "    Where Is Kitty ? 2015-02-25 B+ mod for Tinycat BASIC 2017-01-22"
50 PRINT " 10 rooms (numbered 0 to 9) kitty walks +/- 1 between guesses."
60 PRINT " Usually (80% of time) kitty starts walking in one direction, keeps going"
70 PRINT " in that direction into next room. The dog, that is with you looking for"
80 PRINT " kitty, will tell how many moves ago kitty was in the room with barks,"
90 PRINT " up to 4 Woofs = 4 moves ago, a (dog sighs) means no kitty scent."
95 randomize
100 rem DIM last4moves(4)
101 let lastMove3 = 0
102 let lastMove2 = 0
103 let lastMove1 = 0
104 let lastMove0 = 0
110 GOTO 260
120 REM ============================ SUB move kitty
130 let lastMove3 = lastMove2
140 let lastMove2 = lastMove1
150 let lastMove1 = lastMove0
160 let lastMove0 = room
170 IF RND < 0.8 THEN GOTO 190
180 let direction = -1 * direction
190 let room = room + direction
200 IF room < 10 THEN GOTO 220
210 let room = 0
220 IF room > -1 THEN GOTO 240
230 let room = 9
240 RETURN
250 REM ============================= initialize
260 IF rnd > 0.50 THEN GOTO 290
270 let direction = 1
280 GOTO 300
290 let direction = -1
300 let room = INT(10 * RND)
310 REM move kitty to fill out last4moves arr
320 FOR i = 1 TO 4
330 GOSUB 120
340 NEXT i
350 REM ================================================game
360 PRINT
370 INPUT " Guess the room number 0 to 9, kitty is in (any other quits) enter: ", guess
380 IF guess < 0 OR guess > 9 THEN GOTO 540
390 IF guess = room THEN GOTO 530
400 IF guess = lastMove0 then goto 1000
410 if guess = lastMove1 then goto 2000
420 if guess = lastMove2 then goto 3000
430 if guess = lastMove3 then goto 4000
480 PRINT " (dog sighs)"
490 REM prep next round
500 GOSUB 120
510 GOTO 360
520 REM Kitty found or not say goodbye
530 PRINT " Ah, there's kitty!"
540 PRINT " Goodbye."
550 rem too much temptation not to hit number INPUT " Press Enter ", t
560 END
1000 print " Woof"
1010 goto 490
2000 print " Woof Woof"
2010 goto 490
3000 print " Woof Woof Woof"
3010 goto 490
4000 print " Woof Woof Woof Woof"
4010 goto 490
