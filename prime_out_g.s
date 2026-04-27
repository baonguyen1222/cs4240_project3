.data
  newline: .asciiz "\n"
.text
.globl main
divisible:
  addi $sp, $sp, -16
  sw $ra, 12($sp)
  sw $a0, 0($sp)
  sw $a1, 4($sp)
  lw $t0, 8($sp)
  lw $t1, 0($sp)
  lw $t2, 4($sp)
  div $t1, $t2
  mflo $t0
  mul $t0, $t0, $t2
  sw $t0, 8($sp)
  sw $t1, 0($sp)
  sw $t2, 4($sp)
  bne $t1, $t0, divisible_label0
  sw $t0, 8($sp)
  sw $t1, 0($sp)
  sw $t2, 4($sp)
  li $v0, 1
  lw $ra, 12($sp)
  addi $sp, $sp, 16
  jr $ra
divisible_label0:
  li $v0, 0
  lw $ra, 12($sp)
  addi $sp, $sp, 16
  jr $ra
divisible_end:
  lw $ra, 12($sp)
  addi $sp, $sp, 16
  jr $ra
main:
  addi $sp, $sp, -64
  sw $ra, 60($sp)
  lw $t1, 4($sp)
  lw $t2, 8($sp)
  lw $t3, 12($sp)
  lw $t4, 0($sp)
  lw $t0, 20($sp)
  lw $t5, 16($sp)
  li $v0, 0
  move $t4, $v0
  li $v0, 2
  move $t1, $v0
  li $v0, 3
  move $t2, $v0
  li $v0, 6
  move $t3, $v0
  li $v0, 0
  move $t5, $v0
  sw $t1, 4($sp)
  sw $t2, 8($sp)
  sw $t3, 12($sp)
  sw $t4, 0($sp)
  sw $t0, 20($sp)
  sw $t5, 16($sp)
  jal geti
  lw $t1, 4($sp)
  lw $t2, 8($sp)
  lw $t3, 12($sp)
  lw $t4, 0($sp)
  lw $t0, 20($sp)
  lw $t5, 16($sp)
  move $t0, $v0
  li $v1, 1
  sw $t1, 4($sp)
  sw $t2, 8($sp)
  sw $t3, 12($sp)
  sw $t4, 0($sp)
  sw $t0, 20($sp)
  sw $t5, 16($sp)
  bgt $t0, $v1, main_label0
  sw $t1, 4($sp)
  sw $t2, 8($sp)
  sw $t3, 12($sp)
  sw $t4, 0($sp)
  sw $t0, 20($sp)
  sw $t5, 16($sp)
  lw $t1, 28($sp)
  lw $t0, 24($sp)
  li $v0, 0
  move $t0, $v0
  move $t1, $t0
  sw $t1, 28($sp)
  sw $t0, 24($sp)
  j main_print
  sw $t1, 28($sp)
  sw $t0, 24($sp)
  lw $t0, 20($sp)
  sw $t0, 20($sp)
main_label0:
  li $v1, 3
  sw $t0, 20($sp)
  bgt $t0, $v1, main_label1
  sw $t0, 20($sp)
  lw $t1, 28($sp)
  lw $t0, 24($sp)
  li $v0, 1
  move $t0, $v0
  move $t1, $t0
  sw $t1, 28($sp)
  sw $t0, 24($sp)
  j main_print
  sw $t1, 28($sp)
  sw $t0, 24($sp)
  lw $t2, 28($sp)
  lw $t3, 4($sp)
  lw $t0, 24($sp)
  lw $t1, 32($sp)
  lw $t4, 20($sp)
  lw $t5, 16($sp)
  sw $t2, 28($sp)
  sw $t3, 4($sp)
  sw $t0, 24($sp)
  sw $t1, 32($sp)
  sw $t4, 20($sp)
  sw $t5, 16($sp)
main_label1:
  sw $t2, 28($sp)
  sw $t3, 4($sp)
  sw $t0, 24($sp)
  sw $t1, 32($sp)
  sw $t4, 20($sp)
  sw $t5, 16($sp)
  move $a0, $t4
  move $a1, $t3
  jal divisible
  lw $t2, 28($sp)
  lw $t3, 4($sp)
  lw $t0, 24($sp)
  lw $t1, 32($sp)
  lw $t4, 20($sp)
  lw $t5, 16($sp)
  move $t1, $v0
  move $t0, $t5
  move $t2, $t0
  li $v1, 1
  sw $t2, 28($sp)
  sw $t3, 4($sp)
  sw $t0, 24($sp)
  sw $t1, 32($sp)
  sw $t4, 20($sp)
  sw $t5, 16($sp)
  beq $t1, $v1, main_label2
  sw $t2, 28($sp)
  sw $t3, 4($sp)
  sw $t0, 24($sp)
  sw $t1, 32($sp)
  sw $t4, 20($sp)
  sw $t5, 16($sp)
  lw $t2, 28($sp)
  lw $t0, 24($sp)
  lw $t3, 8($sp)
  lw $t1, 32($sp)
  lw $t4, 20($sp)
  lw $t5, 16($sp)
  sw $t2, 28($sp)
  sw $t0, 24($sp)
  sw $t3, 8($sp)
  sw $t1, 32($sp)
  sw $t4, 20($sp)
  sw $t5, 16($sp)
  move $a0, $t4
  move $a1, $t3
  jal divisible
  lw $t2, 28($sp)
  lw $t0, 24($sp)
  lw $t3, 8($sp)
  lw $t1, 32($sp)
  lw $t4, 20($sp)
  lw $t5, 16($sp)
  move $t1, $v0
  move $t0, $t5
  move $t2, $t0
  li $v1, 1
  sw $t2, 28($sp)
  sw $t0, 24($sp)
  sw $t3, 8($sp)
  sw $t1, 32($sp)
  sw $t4, 20($sp)
  sw $t5, 16($sp)
  beq $t1, $v1, main_label2
  sw $t2, 28($sp)
  sw $t0, 24($sp)
  sw $t3, 8($sp)
  sw $t1, 32($sp)
  sw $t4, 20($sp)
  sw $t5, 16($sp)
  j main_label3
main_label2:
  j main_print
  lw $t0, 0($sp)
  sw $t0, 0($sp)
main_label3:
  li $v0, 5
  move $t0, $v0
  sw $t0, 0($sp)
  lw $t0, 36($sp)
  lw $t1, 0($sp)
  lw $t2, 20($sp)
  sw $t0, 36($sp)
  sw $t1, 0($sp)
  sw $t2, 20($sp)
main_loop:
  mul $t0, $t1, $t1
  sw $t0, 36($sp)
  sw $t1, 0($sp)
  sw $t2, 20($sp)
  bgt $t0, $t2, main_exit
  sw $t0, 36($sp)
  sw $t1, 0($sp)
  sw $t2, 20($sp)
  lw $t2, 28($sp)
  lw $t0, 24($sp)
  lw $t1, 32($sp)
  lw $t5, 0($sp)
  lw $t3, 40($sp)
  lw $t4, 44($sp)
  lw $t6, 20($sp)
  lw $t7, 16($sp)
  sw $t2, 28($sp)
  sw $t0, 24($sp)
  sw $t1, 32($sp)
  sw $t5, 0($sp)
  sw $t3, 40($sp)
  sw $t4, 44($sp)
  sw $t6, 20($sp)
  sw $t7, 16($sp)
  move $a0, $t6
  move $a1, $t5
  jal divisible
  lw $t2, 28($sp)
  lw $t0, 24($sp)
  lw $t1, 32($sp)
  lw $t5, 0($sp)
  lw $t3, 40($sp)
  lw $t4, 44($sp)
  lw $t6, 20($sp)
  lw $t7, 16($sp)
  move $t1, $v0
  move $t0, $t7
  li $v0, 0
  move $t3, $v0
  li $v0, 0
  move $t4, $v0
  move $t2, $t0
  li $v1, 1
  sw $t2, 28($sp)
  sw $t0, 24($sp)
  sw $t1, 32($sp)
  sw $t5, 0($sp)
  sw $t3, 40($sp)
  sw $t4, 44($sp)
  sw $t6, 20($sp)
  sw $t7, 16($sp)
  beq $t1, $v1, main_label2
  sw $t2, 28($sp)
  sw $t0, 24($sp)
  sw $t1, 32($sp)
  sw $t5, 0($sp)
  sw $t3, 40($sp)
  sw $t4, 44($sp)
  sw $t6, 20($sp)
  sw $t7, 16($sp)
  lw $t3, 28($sp)
  lw $t0, 24($sp)
  lw $t1, 32($sp)
  lw $t2, 48($sp)
  lw $t4, 0($sp)
  lw $t5, 20($sp)
  lw $t6, 16($sp)
  li $v1, 2
  add $t2, $t4, $v1
  sw $t3, 28($sp)
  sw $t0, 24($sp)
  sw $t1, 32($sp)
  sw $t2, 48($sp)
  sw $t4, 0($sp)
  sw $t5, 20($sp)
  sw $t6, 16($sp)
  move $a0, $t5
  move $a1, $t2
  jal divisible
  lw $t3, 28($sp)
  lw $t0, 24($sp)
  lw $t1, 32($sp)
  lw $t2, 48($sp)
  lw $t4, 0($sp)
  lw $t5, 20($sp)
  lw $t6, 16($sp)
  move $t1, $v0
  move $t0, $t6
  move $t3, $t0
  li $v1, 1
  sw $t3, 28($sp)
  sw $t0, 24($sp)
  sw $t1, 32($sp)
  sw $t2, 48($sp)
  sw $t4, 0($sp)
  sw $t5, 20($sp)
  sw $t6, 16($sp)
  beq $t1, $v1, main_label2
  sw $t3, 28($sp)
  sw $t0, 24($sp)
  sw $t1, 32($sp)
  sw $t2, 48($sp)
  sw $t4, 0($sp)
  sw $t5, 20($sp)
  sw $t6, 16($sp)
  lw $t0, 0($sp)
  li $v1, 6
  add $t0, $t0, $v1
  sw $t0, 0($sp)
  j main_loop
  sw $t0, 0($sp)
  lw $t1, 28($sp)
  lw $t0, 24($sp)
  lw $t3, 40($sp)
  lw $t2, 52($sp)
  lw $t4, 44($sp)
  sw $t1, 28($sp)
  sw $t0, 24($sp)
  sw $t3, 40($sp)
  sw $t2, 52($sp)
  sw $t4, 44($sp)
main_exit:
  move $t2, $t3
  move $t0, $t4
  li $v0, 1
  move $t0, $v0
  move $t1, $t0
  sw $t1, 28($sp)
  sw $t0, 24($sp)
  sw $t3, 40($sp)
  sw $t2, 52($sp)
  sw $t4, 44($sp)
  lw $t0, 28($sp)
  sw $t0, 28($sp)
main_print:
  move $a0, $t0
  li $v0, 1
  syscall
  li $a0, 10
  li $v0, 11
  syscall
  sw $t0, 28($sp)
main_end:
  li $v0, 10
  syscall

# Helper Intrinsics
geti:
  li $v0, 5
  syscall
  jr $ra
puti:
  li $v0, 1
  syscall
  jr $ra
putc:
  li $v0, 11
  syscall
  jr $ra
