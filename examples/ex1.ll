declare i8* @calloc(i32, i32)
declare i32 @printf(i8*, ...)
declare void @exit(i32)

@_cint = constant [4 x i8] c"%d\0a\00"
@_cOOB = constant [15 x i8] c"Out of bounds\0a\00"
define void @print_int(i32 %i) {
    %_str = bitcast [4 x i8]* @_cint to i8*
    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)
    ret void
}

define void @throw_oob() {
    %_str = bitcast [15 x i8]* @_cOOB to i8*
    call i32 (i8*, ...) @printf(i8* %_str)
    call void @exit(i32 1)
    ret void
}

define i1 @Test2.set(i8* %this, i32 %.x, i32 %.y) {
	%x = alloca i32
	store i32 %.x, i32* %x
	%y = alloca i32
	store i32 %.y, i32* %y
	%a = alloca i32
}
define i32 @Test2.get(i8* %this) {
	%x = alloca i32
	store i32 %.x, i32* %x
	%y = alloca i32
	store i32 %.y, i32* %y
}
