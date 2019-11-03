@.funcs = global [3 x i8*] [i8* bitcast (i32 (i32*, i32*)* @add to i8*),
                            i8* bitcast (i32 (i32*, i32*)* @sub to i8*),
                            i8* bitcast (i32 (i32*, i32*)* @mul to i8*)]

declare i32 @printf(i8*, ...)
@.comp_str = constant [15 x i8] c"%d %c %d = %d\0A\00"
@.ret_val = constant [20 x i8] c"Returned value: %d\0A\00"

define i32 @main() {
    ; allocate local variables
    %ptr_a = alloca i32
    %ptr_b = alloca i32
    %count = alloca i32

    ; initialize var values
    store i32 100, i32* %ptr_a
    store i32 50, i32* %ptr_b
    store i32 0, i32* %count
    br label %loopstart

  loopstart:
    ;load %i from %count
    %i = load i32, i32* %count
    ; while %i < 3
    %fin = icmp slt i32 %i, 3
    br i1 %fin, label %next, label %end

  next:
    ; get pointer to %i'th element of the @.funcs array
    %func_ptr = getelementptr [3 x i8*], [3 x i8*]* @.funcs, i32 0, i32 %i
    ; load %i'th element that contains an i8* to the method
    %func_addr = load i8*, i8** %func_ptr
    ; cast i8* to actual method type in order to call it
    %func = bitcast i8* %func_addr to i32 (i32*, i32*)*
    ; call casted method
    %result = call i32 %func(i32* %ptr_a, i32* %ptr_b)

    ; print result
    %str = bitcast [20 x i8]* @.ret_val to i8*
    call i32 (i8*, ...) @printf(i8* %str, i32 %result)

    ; increase %i and store to %count
    %next_i = add i32 %i, 1
    store i32 %next_i, i32* %count
    ; go to loopstart
    br label %loopstart

  end:
    ret i32 0
}

define i32 @add(i32* %a, i32* %b) {
    %str = bitcast [15 x i8]* @.comp_str to i8*

    ; load values from addresses
    %val_a = load i32, i32* %a
    %val_b = load i32, i32* %b

    ; add them and print the result
    %res = add i32 %val_a, %val_b
    call i32 (i8*, ...) @printf(i8* %str, i32 %val_a, [1 x i8] c"+", i32 %val_b, i32 %res)

    ; return the result
    ret i32 %res
}

define i32 @sub(i32* %a, i32* %b) {
    ; similar as above
    %str = bitcast [15 x i8]* @.comp_str to i8*
    %val_a = load i32, i32* %a
    %val_b = load i32, i32* %b
    %res = sub i32 %val_a, %val_b
    call i32 (i8*, ...) @printf(i8* %str, i32 %val_a, [1 x i8] c"-", i32 %val_b, i32 %res)
    ret i32 %res
}

define i32 @mul(i32* %a, i32* %b) {
    ; similar as above
    %str = bitcast [15 x i8]* @.comp_str to i8*
    %val_a = load i32, i32* %a
    %val_b = load i32, i32* %b
    %res = mul i32 %val_a, %val_b
    call i32 (i8*, ...) @printf(i8* %str, i32 %val_a, [1 x i8] c"*", i32 %val_b, i32 %res)
    ret i32 %res
}
