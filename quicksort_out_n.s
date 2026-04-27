.data
  newline: .asciiz "\n"
.text
.globl main
quicksort:
  addi $sp, $sp, -448
  sw $ra, 444($sp)
  sw $a0, 0($sp)
  sw $a1, 400($sp)
  sw $a2, 404($sp)
  li $t0, 0
  sw $t0, 408($sp)
  li $t0, 0
  sw $t0, 412($sp)
  lw $t0, 400($sp)
  lw $t1, 404($sp)
  bge $t0, $t1, quicksort_end
  lw $t0, 400($sp)
  lw $t1, 404($sp)
  add $t2, $t0, $t1
  sw $t2, 416($sp)
  lw $t0, 416($sp)
  li $t1, 2
  div $t0, $t1
  mflo $t2
  sw $t2, 416($sp)
  lw $t0, 0($sp)
  lw $t1, 416($sp)
  sll $t1, $t1, 2
  add $t0, $t0, $t1
  lw $t2, 0($t0)
  sw $t2, 420($sp)
  lw $t0, 400($sp)
  li $t1, 1
  sub $t2, $t0, $t1
  sw $t2, 408($sp)
  lw $t0, 404($sp)
  li $t1, 1
  add $t2, $t0, $t1
  sw $t2, 412($sp)
quicksort_loop0:
quicksort_loop1:
  lw $t0, 408($sp)
  li $t1, 1
  add $t2, $t0, $t1
  sw $t2, 408($sp)
  lw $t0, 0($sp)
  lw $t1, 408($sp)
  sll $t1, $t1, 2
  add $t0, $t0, $t1
  lw $t2, 0($t0)
  sw $t2, 424($sp)
  lw $t0, 424($sp)
  sw $t0, 428($sp)
  lw $t0, 428($sp)
  lw $t1, 420($sp)
  blt $t0, $t1, quicksort_loop1
quicksort_loop2:
  lw $t0, 412($sp)
  li $t1, 1
  sub $t2, $t0, $t1
  sw $t2, 412($sp)
  lw $t0, 0($sp)
  lw $t1, 412($sp)
  sll $t1, $t1, 2
  add $t0, $t0, $t1
  lw $t2, 0($t0)
  sw $t2, 424($sp)
  lw $t0, 424($sp)
  sw $t0, 432($sp)
  lw $t0, 432($sp)
  lw $t1, 420($sp)
  bgt $t0, $t1, quicksort_loop2
  lw $t0, 408($sp)
  lw $t1, 412($sp)
  bge $t0, $t1, quicksort_exit0
  lw $t0, 0($sp)
  lw $t1, 412($sp)
  lw $t2, 428($sp)
  sll $t1, $t1, 2
  add $t0, $t0, $t1
  sw $t2, 0($t0)
  lw $t0, 0($sp)
  lw $t1, 408($sp)
  lw $t2, 432($sp)
  sll $t1, $t1, 2
  add $t0, $t0, $t1
  sw $t2, 0($t0)
  j quicksort_loop0
quicksort_exit0:
  lw $t0, 412($sp)
  li $t1, 1
  add $t2, $t0, $t1
  sw $t2, 436($sp)
  lw $a0, 0($sp)
  lw $a1, 400($sp)
  lw $a2, 412($sp)
  jal quicksort
  lw $t0, 412($sp)
  li $t1, 1
  add $t2, $t0, $t1
  sw $t2, 412($sp)
  lw $a0, 0($sp)
  lw $a1, 412($sp)
  lw $a2, 404($sp)
  jal quicksort
quicksort_end:
quicksort_end:
  lw $ra, 444($sp)
  addi $sp, $sp, 448
  jr $ra
main:
  addi $sp, $sp, -416
  sw $ra, 412($sp)
  li $t0, 0
  sw $t0, 0($sp)
  jal geti
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
  jal geti
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
  jal quicksort
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
  jal putc
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
