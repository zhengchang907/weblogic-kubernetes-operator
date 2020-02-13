function usage() {
    cat <<EOF
Usage:  
  Pretty prints a WebLogic Kubernetes Operator 2.0 pod log.

  `basename $0` [-?] [-ll severity] [-s] [-v] [-raw] [-n namespace] [-f file|-]

  -?            Show this usage message.

  -ll SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST
                Logging level. Default is WARNING, which includes SEVERE.

  -f logfile|-  Get log from the given log file instead of from a running
                operator. If the file is '-' then read log from stdin.

  -n name-space Operator's namespace. Not needed if there's only one
                operator in your k8s cluster. Ignored if '-f' set.

  -s            Use a single line per log message instead of converting
                '\n' and '\t' to newlines and tabs.

  -v            Print all log message fields instead of a curated subset. 

  -raw          Disable 'pretty print'. Print log messages in their
                entirety and original form, using a single line for each.  

  -k 'args'     Extra parms to add to kubectl log command. For example, 
                -k '-f' streams the log and -k '--tail=10' only looks
                at the last 10 logs. Ignored if '-f' set.
Examples:
  Last 10 log messages from a k8s cluster with a single running operator:
    ./`basename $0` -ll FINEST -k "--tail=10"

  SEVERE+WARNING logs from an operator running in a particular namespace:
    ./`basename $0` -n my-operator-ns

  SEVERE+WARNING logs from a pipelined operator log:
    kubectl -n my-operator-ns logs deployment/weblogic-operator \\
            | ./`basename $0` -f -
EOF
}

k_args=''
raw_mode='false'
newline_conversion='s/\\n/\'$'\n''/g'
tab_conversion='s/\\t/\'$'\t''/g'
log_filter='FINEST|FINER|FINE|CONFIG|INFO'
ns="--all-namespaces=true"
fields_brief='{ print "XXX1" $1 "XXX4\"" $4 "XXX5\"" $5 "XXX9\"" $9 "XXX10\"" $10}'
fields_verbose='{ print "XXX1" $1 "XXX2\"" $2 "XXX3\"" $3 "XXX4\"" $4 "XXX5\"" $5 "XXX6\"" $6 "XXX7\"" $7 "XXX8\"" $8 "XXX9\"" $9 "XXX10\"" $10 "XXX11\"" $11 "XXX12\"" $12 }' 
fields="$fields_brief"
input_file=""

while [ ! -z "$1" ]; do
  case "$1" in
    "-?") usage; exit 1; ;;

    "-n") shift
          ns='-n $1'
          if [ -z "$1" ]; then
            echo "@@ Error, no namespace specified"
            usage; exit 1
          fi
          ;;

    "-f") shift
          input_file="$1"
          if [ -z "$1" ]; then
            echo "@@ Error, no input file specified"
            usage; exit 1
          fi
          if [ ! "$1" = "-" ] && [ ! -f "$1" ]; then
            echo "@@ Error, input file '$1' not found"
            exit 1
          fi
          ;;

    "-ll")shift
          case "$1" in
            FINEST)  log_filter='DOESNOTEXISTFOOBAR' ;;
            FINER)   log_filter='FINEST' ;;
            FINE)    log_filter='FINEST|FINER' ;;
            CONFIG)  log_filter='FINEST|FINER|FINE' ;;
            INFO)    log_filter='FINEST|FINER|FINE|CONFIG' ;;
            WARNING) log_filter='FINEST|FINER|FINE|CONFIG|INFO' ;;
            SEVERE)  log_filter='FINEST|FINER|FINE|CONFIG|INFO|WARNING' ;;
            *)       echo "@@ Error, unrecognized log level '$1'"; usage; exit 1 ;;
          esac
          ;;

    "-s") newline_conversion="s/FOO/FOO/"
          tab_conversion="s/FOO/FOO/"
          ;;

    "-v") fields="$fields_verbose" ;;

    "-raw") raw_mode="true" ;;

    "-k") shift
          k_args="$1"
          if [ -z "$1" ]; then
            echo "@@ Error, no -k params specified"
            usage; exit 1
          fi
          ;;

    *)    echo "@@ Error, unrecognized parameter '$1'"; usage; exit 1 ;;
  esac
  shift
done

if [ "$input_file" = "-" ]; then
  # input is from stdin (a pipe)
  command="cat"
  echo "@@ Reading log from stdin:"

  # In theory, the following can check if there's actually a pipe/redirect:
  # readlink /proc/$$/fd/0 | grep -q "^pipe:"
  # || ! file $( readlink /proc/$$/fd/0 ) | grep -q "character special"A

elif [ ! -z "$input_file" ]; then
  # input is from a file
  command="cat '$input_file'"
  echo "@@ Reading log from file '$input_file':"

else
  # we need to get the pod log from a running operator

  # First, we find the operator pod(s)

  tfile=$(mktemp /tmp/`basename $0`.XXXXXXXXX)

  kubectl get pod \
    $ns \
    -l app=weblogic-operator \
    -o=jsonpath='{range .items[*]}{.kind}{" "}{.metadata.name}{" -n "}{.metadata.namespace}{"\n"}{end}' \
    | grep "^Pod " \
    | sed "s/^Pod //" \
    > $tfile 2>&1

  if [ $? -ne 0 ]; then
    echo "@@ error:  kubectl command failed, error is:" 
    cat $tfile
    rm -f $tfile
    exit 1
  fi

  count=`grep -c '^' $tfile`

  if [ $count -eq 0 ]; then
    echo "@@ error: no operator pod found in namespace '$ns'."
    rm -f $tfile
    usage
    exit 1
  fi

  if [ $count -gt 1 ]; then
    echo "@@ error: more than one operator pod matches namespace '$ns':"
    cat $tfile
    rm -f $tfile
    usage
    exit 1
  fi

  op_pod="`cat $tfile`"
  rm -f $tfile

  command="kubectl logs $op_pod $k_args"

  echo "@@ Reading log from '$command':"
fi


if [ "$raw_mode" = "true" ]; then

  eval "$command"                             | \
    grep '"timestamp"'                        | \
    egrep -v -e "\"level\":\"($log_filter)\"" 

else

  # TBD The awk -F below splits each log messages field by using
  #     ',"' as a delimiter.  This risks improperly splitting if a log
  #     message's contents happen to have that combination of chars.
  #     Maybe this isn't an issue?  Perhaps a '"' is escaped when 
  #     it's in a log message's contents?

  eval "$command"                             | \
    grep '"timestamp"'                        | \
    egrep -v -e "\"level\":\"($log_filter)\"" | \
    sed  's;^.\(.*\).$;\1;'                   | \
    awk  -F ',"' "$fields"                    | \
    sed  's/XXX\([0-9]*\)[^:]*:/ \1:/g'       | \
    sed  's/+0000//'                          | \
    sed  "$newline_conversion"                | \
    sed  "$tab_conversion"

fi

