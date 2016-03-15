echo -e "Result " > Result_$2

for(( w=1; w<=$1; ++w))
do
    echo -e N = $w >> Result_$2
    for (( i=1; i < 5; ++i ))
    do
        for (( j=`expr $i + 1`; j <=5; ++j ))
        do
            ./$2 -f $w.$i $w.$j >> Result_$2
        done
    done
done

