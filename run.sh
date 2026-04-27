#!/bin/bash

# Usage:
# ./run.sh <input_ir> <output_s> [mode]
# Example:
# ./run.sh tests/quicksort.ir out.s --greedy

# 1. Check arguments
if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <input_ir> <output_s> [mode]"
    exit 1
fi

INPUT="$1"
OUTPUT="$2"
MODE=${3:-"--greedy"}

# 2. Run backend (generate MIPS)
echo "=== Generating MIPS ==="
java -cp bin MipsBackend "$INPUT" "$OUTPUT" "$MODE"

if [ $? -ne 0 ]; then
    echo "Backend failed."
    exit 1
fi

# 3. Run MIPS using SPIM
echo "=== Running MIPS ==="
spim -f "$OUTPUT" > tmp_output.txt

# Print output to terminal
cat tmp_output.txt

# 4. Compare with expected output (if exists)
EXPECTED="${INPUT%.ir}.out"

if [ -f "$EXPECTED" ]; then
    echo "=== Comparing with expected ==="
    diff tmp_output.txt "$EXPECTED"

    if [ $? -eq 0 ]; then
        echo "PASS ✅"
    else
        echo "FAIL ❌"
    fi
else
    echo "No expected output file found."
fi

# 5. Cleanup (optional)
# rm tmp_output.txt