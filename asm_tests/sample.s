.data

L0:
    .asciz "%i\n"
L1:
    .asciz "the quick brown fox jumps over the lazy dog\n"

.text

.global main
.type main, %function

main:

ldr v1, =12

ldr v5, =8
str v1, [sp, v5]

ldr v1, =30

ldr v5, =12
str v1, [sp, v5]

ldr v5, =8
ldr v2, [sp, v5]

ldr v5, =12
ldr v3, [sp, v5]

add v1, v2, v3

ldr v5, =16
str v1, [sp, v5]

ldr v5, =16
ldr v1, [sp, v5]

ldr v5, =0
str v1, [sp, v5]

ldr v1, =2

ldr v5, =20
str v1, [sp, v5]

ldr v1, =2

ldr v5, =24
str v1, [sp, v5]

ldr v5, =20
ldr v2, [sp, v5]

ldr v5, =24
ldr v3, [sp, v5]

sub v1, v2, v3

ldr v5, =28
str v1, [sp, v5]

ldr v5, =28
ldr a2, [sp, v5]

ldr v1, =1

ldr v5, =32
str v1, [sp, v5]

ldr v5, =32
ldr a3, [sp, v5]

ldr v1, =0

ldr v5, =36
str v1, [sp, v5]

ldr v5, =36
ldr a4, [sp, v5]

ldr v5, =40
str a3, [sp, v5]

ldr v5, =44
str a4, [sp, v5]

ldr v5, =40
ldr v2, [sp, v5]

ldr v5, =44
ldr v3, [sp, v5]

orr v1, v2, v3

ldr v5, =48
str v1, [sp, v5]

ldr v5, =48
ldr a4, [sp, v5]

ldr v1, =L1

ldr v5, =52
str v1, [sp, v5]

ldr v5, =52
ldr v1, [sp, v5]

ldr v5, =4
str v1, [sp, v5]

ldr v5, =4
ldr v1, [sp, v5]

ldr v5, =56
str v1, [sp, v5]

mov v1, a1

mov v2, a2

ldr v5, =56
ldr a1, [sp, v5]

b printf(PLT)

mov a2, v2

mov a1, v1
