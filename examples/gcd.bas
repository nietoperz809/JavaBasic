10	rem Compute the greatest common divisor (GCD) of two numbers.
20	input "Enter two numbers, please: ", a, b
30	if a > b then let a = a - b
40	if b > a then let b = b - a
50	if a <> b then goto 30
60	print "Greatest Common Divisor of given numbers: ", a
