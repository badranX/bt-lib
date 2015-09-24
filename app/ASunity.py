#!/usr/bin/env python
import os
import shutil
import argparse
import zipfile
import subprocess
import glob


def Swap (unityPath,jarPath):
	"""
	for files in os.listdir(unityPath):
		if files.endswith('.jar') or files.endswith('.jar.meta'):
			os.remove(files)
	"""
	os.chdir(unityPath)
	#files=glob.glob('*.jar')
	files = [f for f in os.listdir(unityPath) if (os.path.isfile(f) & f.endswith('.jar'))]
	#files += glob.glob('*.jar.meta')
	for filename in files:
		print("Deleted : " + filename)
		os.remove(filename)
		
	
	with zipfile.ZipFile(jarPath, 'r') as myzip:
		for name in myzip.namelist():
			if name.endswith("classes.jar"):
				myzip.extract(name, unityPath)
				break
	
	
		
		
if __name__ == '__main__':
	parser = argparse.ArgumentParser(description="Patch Jar file to UnityProject")
	parser.add_argument("unity", type=str, help="Unity Plugin Path")
	parser.add_argument("ASprojectPath", type=str, help="Jar file Path",metavar=('j'))
	args = parser.parse_args()
	
	Swap(args.unity,args.ASprojectPath)
	