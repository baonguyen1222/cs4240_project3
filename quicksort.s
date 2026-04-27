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
  lw $t2, 8($sp)
  lw $t3, 4($sp)
  lw $t0, 12($sp)
  lw $t1, 16($sp)
  li $v0, 0
  move $t0, $v0
  li $v0, 0
  move $t1, $v0
  sw $t2, 8($sp)
  sw $t3, 4($sp)
  sw $t0, 12($sp)
  sw $t1, 16($sp)
  bge $t3, $t2, quicksort_end
  sw $t2, 8($sp)
  sw $t3, 4($sp)
  sw $t0, 12($sp)
  sw $t1, 16($sp)
  lw $t1, 8($sp)
  lw $t6, 0($sp)
  lw $t2, 4($sp)
  lw $t0, 20($sp)
  lw $t3, 24($sp)
  lw $t4, 12($sp)
  lw $t5, 16($sp)
  add $t0, $t2, $t1
  li $v1, 2
  div $t0, $v1
  mflo $t0
  sll $v0, $t0, 2
  add $v0, $t6, $v0
  lw $t3, 0($v0)
  li $v1, 1
  sub $t4, $t2, $v1
  li $v1, 1
  add $t5, $t1, $v1
  sw $t1, 8($sp)
  sw $t6, 0($sp)
  sw $t2, 4($sp)
  sw $t0, 20($sp)
  sw $t3, 24($sp)
  sw $t4, 12($sp)
  sw $t5, 16($sp)
quicksort_loop0:
  lw $t3, 0($sp)
  lw $t1, 28($sp)
  lw $t2, 32($sp)
  lw $t0, 12($sp)
  lw $t4, 24($sp)
quicksort_loop1:
  li $v1, 1
  add $t0, $t0, $v1
  sll $v0, $t0, 2
  add $v0, $t3, $v0
  lw $t2, 0($v0)
  move $t1, $t2
  sw $t3, 0($sp)
  sw $t1, 28($sp)
  sw $t2, 32($sp)
  sw $t0, 12($sp)
  sw $t4, 24($sp)
  blt $t1, $t4, quicksort_loop1
  sw $t3, 0($sp)
  sw $t1, 28($sp)
  sw $t2, 32($sp)
  sw $t0, 12($sp)
  sw $t4, 24($sp)
  lw $t3, 0($sp)
  lw $t1, 36($sp)
  lw $t2, 32($sp)
  lw $t4, 24($sp)
  lw $t0, 16($sp)
quicksort_loop2:
  li $v1, 1
  sub $t0, $t0, $v1
  sll $v0, $t0, 2
  add $v0, $t3, $v0
  lw $t2, 0($v0)
  move $t1, $t2
  sw $t3, 0($sp)
  sw $t1, 36($sp)
  sw $t2, 32($sp)
  sw $t4, 24($sp)
  sw $t0, 16($sp)
  bgt $t1, $t4, quicksort_loop2
  sw $t3, 0($sp)
  sw $t1, 36($sp)
  sw $t2, 32($sp)
  sw $t4, 24($sp)
  sw $t0, 16($sp)
  lw $t0, 12($sp)
  lw $t1, 16($sp)
  sw $t0, 12($sp)
  sw $t1, 16($sp)
  bge $t0, $t1, quicksort_exit0
  sw $t0, 12($sp)
  sw $t1, 16($sp)
  lw $t0, 0($sp)
  lw $t1, 28($sp)
  lw $t2, 36($sp)
  lw $t3, 12($sp)
  lw $t4, 16($sp)
  sll $at, $t4, 2
  add $at, $t0, $at
  sw $t1, 0($at)
  sll $at, $t3, 2
  add $at, $t0, $at
  sw $t2, 0($at)
  sw $t0, 0($sp)
  sw $t1, 28($sp)
  sw $t2, 36($sp)
  sw $t3, 12($sp)
  sw $t4, 16($sp)
  j quicksort_loop0
  sw $t0, 0($sp)
  sw $t1, 28($sp)
  sw $t2, 36($sp)
  sw $t3, 12($sp)
  sw $t4, 16($sp)
  lw $t1, 0($sp)
  lw $t3, 8($sp)
  lw $t4, 4($sp)
  lw $t2, 40($sp)
  lw $t0, 16($sp)
quicksort_exit0:
  li $v1, 1
  add $t2, $t0, $v1
  sw $t1, 0($sp)
  sw $t3, 8($sp)
  sw $t4, 4($sp)
  sw $t2, 40($sp)
  sw $t0, 16($sp)
  move $a0, $t1
  move $a1, $t4
  move $a2, $t0
  jal quicksort
  lw $t1, 0($sp)
  lw $t3, 8($sp)
  lw $t4, 4($sp)
  lw $t2, 40($sp)
  lw $t0, 16($sp)
  li $v1, 1
  add $t0, $t0, $v1
  sw $t1, 0($sp)
  sw $t3, 8($sp)
  sw $t4, 4($sp)
  sw $t2, 40($sp)
  sw $t0, 16($sp)
  move $a0, $t1
  move $a1, $t0
  move $a2, $t3
  jal quicksort
  lw $t1, 0($sp)
  lw $t3, 8($sp)
  lw $t4, 4($sp)
  lw $t2, 40($sp)
  lw $t0, 16($sp)
  sw $t1, 0($sp)
  sw $t3, 8($sp)
  sw $t4, 4($sp)
  sw $t2, 40($sp)
  sw $t0, 16($sp)
quicksort_end:
quicksort_end:
  lw $ra, 44($sp)
  addi $sp, $sp, 48
  jr $ra
main:
  addi $sp, $sp, -24
  sw $ra, 20($sp)
  lw $t1, 0($sp)
  lw $t0, 4($sp)
  li $v0, 0
  move $t1, $v0
  sw $t1, 0($sp)
  sw $t0, 4($sp)
  jal geti
  lw $t1, 0($sp)
  lw $t0, 4($sp)
  move $t0, $v0
  li $v1, 100
  sw $t1, 0($sp)
  sw $t0, 4($sp)
  bgt $t0, $v1, main_return
  sw $t1, 0($sp)
  sw $t0, 4($sp)
  lw $t1, 8($sp)
  lw $t0, 4($sp)
  li $v1, 1
  sub $t0, $t0, $v1
  li $v0, 0
  move $t1, $v0
  sw $t1, 8($sp)
  sw $t0, 4($sp)
  lw $t0, 8($sp)
  lw $t1, 4($sp)
main_loop0:
  sw $t0, 8($sp)
  sw $t1, 4($sp)
  bgt $t0, $t1, main_exit0
  sw $t0, 8($sp)
  sw $t1, 4($sp)
  lw $t2, 12($sp)
  lw $t1, 0($sp)
  lw $t0, 8($sp)
  sw $t2, 12($sp)
  sw $t1, 0($sp)
  sw $t0, 8($sp)
  jal geti
  lw $t2, 12($sp)
  lw $t1, 0($sp)
  lw $t0, 8($sp)
  move $t1, $v0
  sll $at, $t0, 2
  add $at, $t2, $at
  sw $t1, 0($at)
  li $v1, 1
  add $t0, $t0, $v1
  sw $t2, 12($sp)
  sw $t1, 0($sp)
  sw $t0, 8($sp)
  j main_loop0
  sw $t2, 12($sp)
  sw $t1, 0($sp)
  sw $t0, 8($sp)
  lw $t1, 12($sp)
  lw $t0, 8($sp)
  lw $t2, 4($sp)
main_exit0:
  sw $t1, 12($sp)
  sw $t0, 8($sp)
  sw $t2, 4($sp)
  move $a0, $t1
  li $a1, 0
  move $a2, $t2
  jal quicksort
  lw $t1, 12($sp)
  lw $t0, 8($sp)
  lw $t2, 4($sp)
  li $v0, 0
  move $t0, $v0
  sw $t1, 12($sp)
  sw $t0, 8($sp)
  sw $t2, 4($sp)
  lw $t0, 8($sp)
  lw $t1, 4($sp)
main_loop1:
  sw $t0, 8($sp)
  sw $t1, 4($sp)
  bgt $t0, $t1, main_exit1
  sw $t0, 8($sp)
  sw $t1, 4($sp)
  lw $t2, 12($sp)
  lw $t1, 0($sp)
  lw $t0, 8($sp)
  sll $v0, $t0, 2
  add $v0, $t2, $v0
  lw $t1, 0($v0)
  move $a0, $t1
  li $v0, 1
  syscall
  li $a0, 10
  li $v0, 11
  syscall
  li $v1, 1
  add $t0, $t0, $v1
  sw $t2, 12($sp)
  sw $t1, 0($sp)
  sw $t0, 8($sp)
  j main_loop1
  sw $t2, 12($sp)
  sw $t1, 0($sp)
  sw $t0, 8($sp)
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
