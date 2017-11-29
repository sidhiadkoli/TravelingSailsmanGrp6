import subprocess, os, sys
import numpy as np
from collections import defaultdict
import operator
import pprint



def save_float(x):
    try:
        return float(x)
    except ValueError:
        return None




def analyzeResults(resultsList, repetition):
    """
        Description: Simply analyzes the results saved in resultsList which should contain results for every run
            and is expected to be of length repitition
        Inputs:
            resultsList (list(dict)): list of dictionaries which are the results of the runs.
            repition (int): how many times was the game run
        Outputs:
            None: Just prints some dank results
        TODO:
    """
    # print(resultsList)
    pp = pprint.PrettyPrinter(indent=4)
    playerPlacementsDict = defaultdict(int)
    payerScoreDict = defaultdict(int)
    # pp.pprint(resultsList)
    for results in resultsList:
        sortedResults = sorted(results.items(), key=operator.itemgetter(1), reverse = True)
        for placement, playerTup in enumerate(sortedResults):
            player = playerTup[0]
            score = playerTup[1]# this is not used rn
            # print(player)
            payerScoreDict[player] += score/repetition
            if placement == 0:
                playerPlacementsDict[player] += 1/repetition
            # playerPlacementsDict[player] += placement
        # pp.pprint(sortedResults)
    print("*"*100)
    print("Placement scores")
    pp.pprint(dict(playerPlacementsDict))
    print("\n\n Average Score")
    pp.pprint(dict(payerScoreDict))


repetition = 100



allResultsList = [-1]*repetition
results = {}


for run in range(repetition):
    p = open("tmp.log", "w")
    err = open("err.log", "w")
    subprocess.run(["make", "run"],stdout = p, stderr = err)
    scale = None
    with open("tmp.log", "r") as log:
        defaultPlayerCount = 0
        for line in log:
            # print(line)
            # print(len(line))
            if len(line.strip()) == 1:
                try:
                    scale = int(line.strip())
                except:
                    scale = None
            if " scored " in line:
                # print(line)
                line = line.strip()
                # print(line)
                roundResults = line.split()
                player = roundResults[0]
                if player == "default1":
                    player = player + "_{}".format(defaultPlayerCount)
                    defaultPlayerCount += 1
                score = int(float(roundResults[2]))
                results[player] = score
        # 1/0
    allResultsList[run] = dict(results)
# print(allResultsList)
print("N runs is {}\nwith scale {}".format(repetition, scale))
analyzeResults(resultsList = allResultsList, repetition = repetition)






