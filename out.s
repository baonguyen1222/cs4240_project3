.data
  newline: .asciiz "\n"
.text
.globl main
quicksort:
  addi $sp, $sp, -48
  sw $ra, 44($sp)
  sw $a0, 0($sp)
  sw $a1, 4($sp)
  sw $a2, 8($sp)
  li $t0, 0
  sw $t0, 12($sp)
  li $t0, 0
  sw $t0, 16($sp)
  lw $t0, 4($sp)
  lw $t1, 8($sp)
  bge $t0, $t1, quicksort_end
  lw $t0, 4($sp)
  lw $t1, 8($sp)
  add $t2, $t0, $t1
  sw $t2, 20($sp)
  lw $t0, 20($sp)
  li $t1, 2
  div $t0, $t1
  mflo $t2
  sw $t2, 20($sp)
  lw $t0, 0($sp)
  lw $t1, 20($sp)
  sll $t1, $t1, 2
  add $t0, $t0, $t1
  lw $t2, 0($t0)
  sw $t2, 24($sp)
  lw $t0, 4($sp)
  li $t1, 1
  sub $t2, $t0, $t1
  sw $t2, 12($sp)
  lw $t0, 8($sp)
  li $t1, 1
  add $t2, $t0, $t1
  sw $t2, 16($sp)
quicksort_loop0:
quicksort_loop1:
  lw $t0, 12($sp)
  li $t1, 1
  add $t2, $t0, $t1
  sw $t2, 12($sp)
  lw $t0, 0($sp)
  lw $t1, 12($sp)
  sll $t1, $t1, 2
  add $t0, $t0, $t1
  lw $t2, 0($t0)
  sw $t2, 28($sp)
  lw $t0, 28($sp)
  sw $t0, 32($sp)
  lw $t0, 32($sp)
  lw $t1, 24($sp)
  blt $t0, $t1, quicksort_loop1
quicksort_loop2:
  lw $t0, 16($sp)
  li $t1, 1
  sub $t2, $t0, $t1
  sw $t2, 16($sp)
  lw $t0, 0($sp)
  lw $t1, 16($sp)
  sll $t1, $t1, 2
  add $t0, $t0, $t1
  lw $t2, 0($t0)
  sw $t2, 28($sp)
  lw $t0, 28($sp)
  sw $t0, 36($sp)
  lw $t0, 36($sp)
  lw $t1, 24($sp)
  bgt $t0, $t1, quicksort_loop2
  lw $t0, 12($sp)
  lw $t1, 16($sp)
  bge $t0, $t1, quicksort_exit0
  lw $t0, 0($sp)
  lw $t1, 16($sp)
  lw $t2, 32($sp)
  sll $t1, $t1, 2
  add $t0, $t0, $t1
  sw $t2, 0($t0)
  lw $t0, 0($sp)
  lw $t1, 12($sp)
  lw $t2, 36($sp)
  sll $t1, $t1, 2
  add $t0, $t0, $t1
  sw $t2, 0($t0)
  j quicksort_loop0
quicksort_exit0:
  lw $t0, 16($sp)
  li $t1, 1
  add $t2, $t0, $t1
  sw $t2, 40($sp)
  lw $a0, 0($sp)
  lw $a1, 4($sp)
  lw $a2, 16($sp)
  addi $sp, $sp, -4
  sw $ra, 0($sp)
  jal quicksort
  lw $ra, 0($sp)
  addi $sp, $sp, 4
  lw $t0, 16($sp)
  li $t1, 1
  add $t2, $t0, $t1
  sw $t2, 16($sp)
  lw $a0, 0($sp)
  lw $a1, 16($sp)
  lw $a2, 8($sp)
  addi $sp, $sp, -4
  sw $ra, 0($sp)
  jal quicksort
  lw $ra, 0($sp)
  addi $sp, $sp, 4
quicksort_end:
quicksort_end:
  lw $ra, 44($sp)
  addi $sp, $sp, 48
  jr $ra
main:
  addi $sp, $sp, -24
  sw $ra, 20($sp)
  li $t0, 0
  sw $t0, 0($sp)
  addi $sp, $sp, -4
  sw $ra, 0($sp)
  jal geti
  lw $ra, 0($sp)
  addi $sp, $sp, 4
  sw $v0, 4($sp)
  lw $t0, 4($sp)
  li $t1, 100
  bgt $t0, $t1, main_return
  lw $t0, 4($sp)
  li $t1, 1
  sub $t2, $t0, $t1
  sw $t2, 4($sp)
  li $t0, 0
  sw $t0, 8($sp)
main_loop0:
  lw $t0, 8($sp)
  lw $t1, 4($sp)
  bgt $t0, $t1, main_exit0
  addi $sp, $sp, -4
  sw $ra, 0($sp)
  jal geti
  lw $ra, 0($sp)
  addi $sp, $sp, 4
  sw $v0, 0($sp)
  lw $t0, 12($sp)
  lw $t1, 8($sp)
  lw $t2, 0($sp)
  sll $t1, $t1, 2
  add $t0, $t0, $t1
  sw $t2, 0($t0)
  lw $t0, 8($sp)
  li $t1, 1
  add $t2, $t0, $t1
  sw $t2, 8($sp)
  j main_loop0
main_exit0:
  lw $a0, 12($sp)
  li $a1, 0
  lw $a2, 4($sp)
  addi $sp, $sp, -4
  sw $ra, 0($sp)
  jal quicksort
  lw $ra, 0($sp)
  addi $sp, $sp, 4
  li $t0, 0
  sw $t0, 8($sp)
main_loop1:
  lw $t0, 8($sp)
  lw $t1, 4($sp)
  bgt $t0, $t1, main_exit1
  lw $t0, 12($sp)
  lw $t1, 8($sp)
  sll $t1, $t1, 2
  add $t0, $t0, $t1
  lw $t2, 0($t0)
  sw $t2, 0($sp)
  lw $a0, 0($sp)
  li $v0, 1
  syscall
  li $a0, 10
  addi $sp, $sp, -4
  sw $ra, 0($sp)
  jal putc
  lw $ra, 0($sp)
  addi $sp, $sp, 4
  lw $t0, 8($sp)
  li $t1, 1
  add $t2, $t0, $t1
  sw $t2, 8($sp)
  j main_loop1
main_exit1:
main_return:
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
