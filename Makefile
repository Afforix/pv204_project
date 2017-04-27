.PHONY: slides all clean

all: slides

slides: presentation.md
	pandoc -t beamer -o presentation.pdf -V theme:fibeamer presentation.md

clean:
	rm presentation.pdf
