LATEX= latex
BIBTEX= bibtex
DVIPDF= dvipdfmx

REPORT= omnet

.PHONY: all
all: bib dvi pdf

.PHONY: bib
bib: dvi
	@$(BIBTEX) $(REPORT)
	@$(LATEX) $(REPORT)

.PHONY: dvi
dvi:
	@$(LATEX) $(REPORT)

.PHONY: pdf
pdf: bib dvi
	@$(LATEX) $(REPORT)
	$(DVIPDF) $(REPORT)

.PHONY: clean
clean:
	@$(RM) *.pdf *.aux *.log *.bbl *~ *.blg *.dvi
