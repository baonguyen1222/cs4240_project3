#!/bin/bash

# This script runs the MipsBackend.
# It takes 3 arguments: <input_ir> <output_s> <mode>
# Example: ./run.sh input.ir output.s --greedy

# Ensure we have the correct number of arguments for the Backend
if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <input_ir> <output_s> [mode]"
    echo "Defaulting to --greedy mode if no mode is specified."
    MODE="--greedy"
else
    # If the user provides a mode (like --naive), use it; otherwise default to --greedy
    MODE=${3:-"--greedy"}
fi

# Run the backend
# -cp bin: points to the compiled classes
# "$1" is the input IR, "$2" is the output MIPS, "$MODE" is the allocation strategy
java -cp bin MipsBackend "$1" "$2" "$MODE"