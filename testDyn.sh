echo -e "Result "$1-$2 >> Result_$3
echo Testing $3 avec $1-$2
for(( w=1; w<=9; ++w))
do
    for(( q=1; q<=10; ++q))
    do
        java $3 -c -f WC-$1-$2-0$w.txt >> Result_$3
    done
done
for(( q=1; q<=10; ++q))
do
    java $3 -f WC-$1-$2-10.txt >> Result_$3
done
