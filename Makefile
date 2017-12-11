g=5 g6 g6a g6c g6e g6f
fps=1000
t=25
frameskip=1
S=10
timelimit = 2500
timestep = 0.015
all: compile

compile:
	javac sail/sim/Simulator.java

gui:
	java sail.sim.Simulator -tl ${timelimit} -dt ${timestep} --verbose --fps ${fps} --gui -g ${g} -t ${t} --frameskip ${frameskip}

run:
	java sail.sim.Simulator -g ${g} -t ${t} -tl ${timelimit} -dt ${timestep}

verbose:
	java sail.sim.Simulator -g ${g}  -t ${t} --verbose -tl ${timelimit} -dt ${timestep}
