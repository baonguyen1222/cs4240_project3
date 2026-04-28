.data
  newline: .asciiz "\n"
.text
.globl main
divisible:
  addi $sp, $sp, -16
  sw $ra, 12($sp)
  sw $a0, 0($sp)
  sw $a1, 4($sp)
  lw $t0, 0($sp)
  lw $t1, 4($sp)
  div $t0, $t1
  mflo $t2
  sw $t2, 8($sp)
  lw $t0, 8($sp)
  lw $t1, 4($sp)
  mul $t2, $t0, $t1
  sw $t2, 8($sp)
  lw $t0, 0($sp)
  lw $t1, 8($sp)
  bne $t0, $t1, divisible_label0
  li $v0, 1
  j divisible_exit
divisible_label0:
  li $v0, 0
  j divisible_exit
divisible_exit:
  lw $ra, 12($sp)
  addi $sp, $sp, 16
  jr $ra
main:
  addi $sp, $sp, -64
  sw $ra, 60($sp)
  li $t0, 0
  sw $t0, 0($sp)
  li $t0, 2
  sw $t0, 4($sp)
  li $t0, 3
  sw $t0, 8($sp)
  li $t0, 6
  sw $t0, 12($sp)
  li $t0, 0
  sw $t0, 16($sp)
  li $v0, 5
  syscall
  sw $v0, 20($sp)
  lw $t0, 20($sp)
  li $t1, 1
  bgt $t0, $t1, main_label0
  li $t0, 0
  sw $t0, 24($sp)
  lw $t0, 24($sp)
  sw $t0, 28($sp)
  j main_print
main_label0:
  lw $t0, 20($sp)
  li $t1, 3
  bgt $t0, $t1, main_label1
  li $t0, 1
  sw $t0, 24($sp)
  lw $t0, 24($sp)
  sw $t0, 28($sp)
  j main_print
main_label1:
  lw $a0, 20($sp)
  lw $a1, 4($sp)
  jal divisible
  sw $v0, 32($sp)
  lw $t0, 16($sp)
  sw $t0, 24($sp)
  lw $t0, 24($sp)
  sw $t0, 28($sp)
  lw $t0, 32($sp)
  li $t1, 1
  beq $t0, $t1, main_label2
  lw $a0, 20($sp)
  lw $a1, 8($sp)
  jal divisible
  sw $v0, 32($sp)
  lw $t0, 16($sp)
  sw $t0, 24($sp)
  lw $t0, 24($sp)
  sw $t0, 28($sp)
  lw $t0, 32($sp)
  li $t1, 1
  beq $t0, $t1, main_label2
  j main_label3
main_label2:
  j main_print
main_label3:
  li $t0, 5
  sw $t0, 0($sp)
main_loop:
  lw $t0, 0($sp)
  lw $t1, 0($sp)
  mul $t2, $t0, $t1
  sw $t2, 36($sp)
  lw $t0, 36($sp)
  lw $t1, 20($sp)
  bgt $t0, $t1, main_exit
  lw $a0, 20($sp)
  lw $a1, 0($sp)
  jal divisible
  sw $v0, 32($sp)
  lw $t0, 16($sp)
  sw $t0, 24($sp)
  li $t0, 0
  sw $t0, 40($sp)
  li $t0, 0
  sw $t0, 44($sp)
  lw $t0, 24($sp)
  sw $t0, 28($sp)
  lw $t0, 32($sp)
  li $t1, 1
  beq $t0, $t1, main_label2
  lw $t0, 0($sp)
  li $t1, 2
  add $t2, $t0, $t1
  sw $t2, 48($sp)
  lw $a0, 20($sp)
  lw $a1, 48($sp)
  jal divisible
  sw $v0, 32($sp)
  lw $t0, 16($sp)
  sw $t0, 24($sp)
  lw $t0, 24($sp)
  sw $t0, 28($sp)
  lw $t0, 32($sp)
  li $t1, 1
  beq $t0, $t1, main_label2
  lw $t0, 0($sp)
  li $t1, 6
  add $t2, $t0, $t1
  sw $t2, 0($sp)
  j main_loop
main_exit:
  lw $t0, 40($sp)
  sw $t0, 52($sp)
  lw $t0, 44($sp)
  sw $t0, 24($sp)
  li $t0, 1
  sw $t0, 24($sp)
  lw $t0, 24($sp)
  sw $t0, 28($sp)
main_print:
  lw $a0, 28($sp)
  li $v0, 1
  syscall
  li $a0, 10
  li $v0, 11
  syscall
main_exit:
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
