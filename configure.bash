# Determine the Instrumentation home directory based on the location of
# this script...
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  # if $SOURCE was a relative symlink, we need to resolve it relative to the
  # path where the symlink file was located
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
INST_HOME="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# setup PATH and JAVA_HOME if on hippo
if [[ "$(hostname)" == "hippo" ]]
then
  export JAVA_HOME=/afs/csail/group/pag/software/pkg/jdk-7
  export PATH=$JAVA_HOME/bin:$PATH
fi

# function for constructing classpath from specified jar directory
build_classpath ()
{
  IFS=$'\t\n'
  files=($(find $1 -name "*.jar"))
  unset $IFS #or IFS=$' \t\n'

  i=0
  for f in ${files[@]}
  do
    # dangerous.jar is not a real jar file
    if [[ "$(basename $f)" == "dangerous.jar" ]]; then
       continue
    fi

    if [[ $i > 0 ]]
    then
       echo -n :\"$f\"
    else
       echo -n \"$f\"
    fi
    ((i++))
  done
}

# setup the convenient aliases
bootpath="$(build_classpath ~/.cleartrack/jre):$(build_classpath "$INST_HOME"/lib):\"$INST_HOME\"/cleartrack.jar:\"\$PWD\"/output-inst/ss-config.jar"
alias javai="java -Xbootclasspath:$bootpath"
alias javapi="javap -Xbootclasspath:$bootpath"

cleartrack() {
  java -javaagent:$INST_HOME/lib/inliner.jar \
	-Xbootclasspath/a:"$INST_HOME"/lib/inliner.jar:"$INST_HOME"/lib/asm-debug-all-5.0.3.jar \
	-Xmx3g -jar "$INST_HOME"/cleartrack.jar "$@"
  which dot >/dev/null
  if [[ $? == 0 ]];
  then
    if [[ -f "./output-inst/jardeps.dot" ]];
    then
      dot -Tpng output-inst/jardeps.dot > output-inst/jardeps.png
    fi
  fi
}

# implement bash completion for junit.sh script
_junit()
{
    local cur prev opts
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    dir=$(echo ${cur} | tr "." "/")
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    dirs=$(dirname test/${dir} 2>/dev/null)
    opts=$(for opt in test/${dir}*; do if [[ -d ${opt} ]]; then echo ${opt:5} | tr "/" "." ; fi; done)
    opts="$opts "$(for opt in test/${dir}*Test.java; do if [[ -f ${opt} ]]; then echo ${opt:5} | tr "/" "." | sed -e "s/\.java$//g" ; fi; done)

    COMPREPLY=( $(compgen -W "${opts}" -- ${cur}) )
}
complete -F _junit -o nospace ./junit.sh
