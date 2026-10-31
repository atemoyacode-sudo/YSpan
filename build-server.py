from pathlib import Path
import argparse,os,shutil,subprocess
root=Path(__file__).resolve().parent
parser=argparse.ArgumentParser(description="Build the Paper companion plugin using separately obtained dependencies.")
parser.add_argument("--java-home",default=os.environ.get("JAVA_HOME"))
parser.add_argument("--paper-api",type=Path,required=True)
parser.add_argument("--viaversion",type=Path,required=True)
parser.add_argument("--libraries",type=Path,required=True,help="Directory containing Paper runtime dependency JARs")
args=parser.parse_args()
def executable(name):
    if args.java_home:return str(Path(args.java_home)/"bin"/name)
    found=shutil.which(name)
    if not found:parser.error("Set JAVA_HOME or --java-home to a JDK 25 installation")
    return found
cp=[args.paper_api,args.viaversion]+sorted(args.libraries.rglob("*.jar"))
if not all(p.is_file() for p in cp):parser.error("A dependency JAR is missing")
classes=root/"server"/"classes";classes.mkdir(parents=True,exist_ok=True)
subprocess.run([executable("javac"),"--release","25","-cp",os.pathsep.join(map(str,cp)),"-d",str(classes)]+[str(p) for p in (root/"server"/"src").rglob("*.java")],check=True)
output=root/"server"/"BelowZeroBridge-0.1.0-experimental.jar"
subprocess.run([executable("jar"),"cf",str(output),"-C",str(classes),".","-C",str(root/"server"),"plugin.yml","-C",str(root/"server"),"config.yml"],check=True)
print(output)
