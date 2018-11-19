type reg = string

(* empty for default (word), B (byte), 
   SH (signed halfword), H (unsigned halfword), 
   D (doubleword) 
*)
type word_type = string

type label = string

type address_type = 
	| LabelAddr of string
	| Reg of reg
	| RegPreIndexed of reg * int * bool
	| RegPostIndexed of reg * int

type cond = string

(* Operand2 has two possible forms *)
(* #immed_8r (8-bit numeric constant) or Rm (register) *)
type operand2_type = 
	| ImmedOp of string
	| RegOp of reg

(* Memory Access Instructions: LDR or STR
 op{cond}type Rd, [Rn]
 op{cond}type Rd, [Rn, Offset]{!}
 op{cond}type Rd, label
 op{cond}type Rd, [Rn], Offset
*)
type mem_instr_type = 
	cond * word_type * reg * address_type 

(* General Data Processing instruction type *)
(* op{cond}{S} Rd, Rn, Operand2 *)
(* cond is an optional condition code *)
(* S is an optional suffix to denote 
	the updating of condition flags on the result *)
type data_instr_type = 
	cond * bool * reg * reg * operand2_type
	
type multiply_instr_type = 
	cond * bool * reg * reg * reg
(* 
   MOV{cond}{S} Rd, Operand2
   MVN{cond}{S} Rd, Operand2 
*)
type mov_instr_type = 
	cond * bool * reg * operand2_type

(* 
  CMP{cond} Rn, Operand2
  CMN{cond} Rn, Operand2
  TST{cond} Rn, Operand2
  TEQ{cond} Rn, Operand2
*)
type cmp_instr_type = cond * reg * operand2_type

type arm_instr = 
	| PseudoInstr of string
	| Label of string
	| LDR of mem_instr_type 
	| STR of  mem_instr_type
	| LDMFD of (reg list)
	| STMFD of (reg list)
	| ADD of data_instr_type
	| SUB of data_instr_type
	| RSB of data_instr_type
	| AND of data_instr_type
	| ORR of data_instr_type
	| EOR of data_instr_type
	| MOV of mov_instr_type
	| MVN of mov_instr_type
	| CMP of cmp_instr_type
	| CMN of cmp_instr_type
	| TST of cmp_instr_type
	| TEQ of cmp_instr_type
	| B of cond * label
	| BL of cond * label
	| BX of cond * reg
	| MUL of multiply_instr_type
	
type arm_program = arm_instr list


