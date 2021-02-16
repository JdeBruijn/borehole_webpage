#Setup CLASSPATH to include all files in 'lib' folder.
#Change value of 'libs_path' variable to use a different location for libraries.
make clean

export libs_path="$(pwd)/WEB-INF/lib/"
export classes="WEB-INF/classes/"
export libs=$(find "$libs_path" -type f)
export libs_conc=$(sed ':a;N;$!ba;s/\n/:/g'  <<< "$libs")
export CLASSPATH="$classes:$libs_conc"

echo "CLASSPATH=$CLASSPATH" ##debug**
make
echo -e "\nBuild complete."


