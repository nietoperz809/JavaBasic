10 rem Average Program
20 rem This program is very average.
30 input "How many numbers do you want me to average out? ", iteration
40 let count = 0
50 let total = 0
60 input "Please give me a number! ", a
70 let total = total + a
80 let count = count + 1
90 if count < iteration then goto 60
100 let average = total / iteration
110 print "You gave me ", iteration, " numbers, and the average of them is ", average, "! Have a nice day."
