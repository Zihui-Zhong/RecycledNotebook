echo -e "Result "$1-$2 >> Result_$3
echo Testing $3 avec $1-$2
for(( w=1; w<=9; ++w))
do
    java $3 -f WC-$1-$2-0$w.txt >> Result_$3
done
java $3 -f WC-$1-$2-10.txt >> Result_$3
