# VHDL combinational circuit compiler

Created for labs for Compiler class. 


Vhdl 
| Splitter and elaborater
|
Vhdl
|
|V parboiled parser
|
F combinational language
|
|F parboiled parser
|
Fprogram AST
|
| ------------------------------
|	 			|
|			Mapper(creates combinational circuit diagram)
|
|  stimulus W waveform language
|	|
|	|W parboiled parser
|	|
|--------
|
Simulator(uses F program AST) HAS TWO VERSIONS(x86 asm and java simulators)
|
output W waveform
