# Combinational VHDL compiler
- Multilayered approach to design which takes a VHDL program and test stimulus as input and outputs a stream of waveform bits and a synthesized circuit.
- Only Supports combinational synthesis and simulation. Expansion into sequential circuits are a future endeavor.

## Parsing
uses Java parboiled parsers, to parse languages. The multiple languages are parsed inside the compiler. The variations of parsers inside the compiler are Vhdl parser, F(boolean algebra form), W (for stimulus), SVG(for sythesis mapping reverse), and Graphviz(for synthesizing). 

## Testing Parser
the methodology to test these were to implement back and forth transformations to find the same result. Given the correct result of output, we should be able to transform our input into that output to match. 

## Testing synthesizer
The synthesis map is in SVG, since synthesis does not lose critical information. Transforming from an F language to a synthesis diagram back and forth should yield the same result otherwise there is a parser or synthesizer problem.

## Simulation
This uses an indirect simulation method. Given a Vhdl program and its corresponding stimulus in Waveform. The Compiler creates a simulator program.
Two options for simulating: 
1. x86 asm: best performance but longer compile time depending on the length of the simulation
2. Java program: slower to run but better but faster to compile into. 

