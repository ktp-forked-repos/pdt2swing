:- module(tag, []).
:- dynamic(current_id/1).
current_id(4).
:- dynamic(tag/2).
tag(1, 'Fantasy').
tag(2, 'Harry Potter').
tag(3, 'Boxing').
tag(4, 'Great').
